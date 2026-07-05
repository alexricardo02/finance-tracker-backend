package com.example.dataTransferObjects;

public class ExchangeRateResponseDTO {
	private Double compra;
    private Double venta;
    private String casa;
    private String fechaActualizacion;

    public Double getCompra() { return compra; }
    public void setCompra(Double compra) { this.compra = compra; }
    public Double getVenta() { return venta; }
    public void setVenta(Double venta) { this.venta = venta; }
    public String getCasa() { return casa; }
    public void setCasa(String casa) { this.casa = casa; }
    public String getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(String fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
