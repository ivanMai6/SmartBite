package xyz.ivan.aisearch.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "merchants")
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String category;

    @Column(name = "avg_price")
    private Integer avgPrice;

    private BigDecimal rating;
    private String address;

    @Column(columnDefinition = "TEXT") // 确保长文本能存下
    private String description;

    // 为了简化，经纬度先分开存
    private Double latitude;
    private Double longitude;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
