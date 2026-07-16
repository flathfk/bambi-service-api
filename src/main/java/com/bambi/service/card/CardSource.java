package com.bambi.service.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 카드 출처 (service.card_sources). 카드 1장에 여러 출처.
 * 출처 없는 카드 금지 원칙의 물리적 근거 테이블.
 */
@Entity
@Table(name = "card_sources")
public class CardSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(length = 500)
    private String title;

    @Column
    private String url;

    protected CardSource() {
    }

    CardSource(Card card, String title, String url) {
        this.card = card;
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }
}
