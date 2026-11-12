package edu.itc.shop.domain;

// JPA
import jakarta.persistence.Embeddable;

@Embeddable
public record Address(String street, String city, String zip) {

}
