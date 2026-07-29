package com.bambi.service.like;

import com.bambi.service.card.Card;
import com.bambi.service.card.CardRepository;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.like.dto.LikeResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link LikeService} 검증 — 공개 카드만 좋아요 / 멱등 좋아요.
 */
class LikeServiceTest {

    private final LikeRepository likeRepository = mock(LikeRepository.class);
    private final CardRepository cardRepository = mock(CardRepository.class);
    private final LikeService service = new LikeService(likeRepository, cardRepository);

    @Test
    void 비공개_카드는_좋아요할_수_없다_NOT_FOUND() {
        Card privateCard = mock(Card.class);
        when(privateCard.getVisibility()).thenReturn("PRIVATE");
        when(cardRepository.findByPublicIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(privateCard));

        ApiException ex = catchThrowableOfType(
                () -> service.like(1L, UUID.randomUUID().toString()), ApiException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        verify(likeRepository, never()).insertIgnore(anyLong(), anyLong());
    }

    @Test
    void 존재하지_않는_카드_좋아요는_NOT_FOUND() {
        when(cardRepository.findByPublicIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> service.like(1L, UUID.randomUUID().toString()), ApiException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 공개_카드_좋아요는_멱등이고_총수를_돌려준다() {
        Card publicCard = mock(Card.class);
        when(publicCard.getVisibility()).thenReturn("PUBLIC");
        when(publicCard.getId()).thenReturn(10L);
        when(cardRepository.findByPublicIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(publicCard));
        when(likeRepository.countByCardId(10L)).thenReturn(3L);

        LikeResponse res = service.like(1L, UUID.randomUUID().toString());

        assertThat(res.liked()).isTrue();
        assertThat(res.likeCount()).isEqualTo(3L);
        verify(likeRepository).insertIgnore(1L, 10L);
    }
}
