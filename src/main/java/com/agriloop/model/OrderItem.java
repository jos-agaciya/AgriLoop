package com.agriloop.model;

import java.math.BigDecimal;

/**
 * Line item within a waste purchase Order.
 */
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long listingId;
    private BigDecimal quantityTons;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    // Transient helper
    private String listingTitle;

    public OrderItem() {}

    public OrderItem(Long listingId, BigDecimal quantityTons, BigDecimal unitPrice) {
        this.listingId = listingId;
        this.quantityTons = quantityTons;
        this.unitPrice = unitPrice;
        this.subtotal = quantityTons.multiply(unitPrice);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }

    public BigDecimal getQuantityTons() { return quantityTons; }
    public void setQuantityTons(BigDecimal quantityTons) { this.quantityTons = quantityTons; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }
}
