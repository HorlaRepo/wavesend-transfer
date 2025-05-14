package com.shizzy.moneytransfer.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Data
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity(name = "Countries")
@Table(name = "countries")
@Builder
public class Country implements Serializable {

    @Serial
    private static final long serialVersionUID = 5L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long countryId;

    @Column(unique = true, nullable = false)
    private String name;

    @Column
    private String acronym;

    @Column
    private String currency;

    private Integer rating;

    @Column
    private String region;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinTable(
            name = "country_mobile_money_options",
            joinColumns = @JoinColumn(name = "country_id"),
            inverseJoinColumns = @JoinColumn(name = "mobile_money_option_id")
    )
    private Set<MobileMoneyOption> mobileMoneyOption;
}
