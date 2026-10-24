package com.minerva.infrastructure.bootstrap;

import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.constants.SaleType;
import com.minerva.domain.entities.product.Product;
import com.minerva.domain.entities.stockEntry.StockEntry;
import com.minerva.domain.entities.stockReceipt.StockReceipt;
import com.minerva.domain.entities.supplier.Supplier;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.ProductRepository;
import com.minerva.domain.repositories.SupplierRepository;
import com.minerva.domain.valueObject.SupplierName;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("demo")
@ConditionalOnProperty(name = "minerva.demo.catalog.enabled", havingValue = "true")
public class DemoCatalogBootstrap implements ApplicationRunner {
    private final SupplierRepository suppliers;
    private final ProductRepository products;

    public DemoCatalogBootstrap(SupplierRepository suppliers, ProductRepository products) {
        this.suppliers = suppliers;
        this.products = products;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        var supplierByName = new java.util.HashMap<String, Supplier>();
        for (String name : SUPPLIER_NAMES) {
            SupplierName value = new SupplierName(name);
            Supplier supplier = suppliers.findBySupplierName(value).orElseGet(() -> createSupplier(name));
            supplierByName.put(name, supplier);
        }

        List<Product> existing = new ArrayList<>(products.findAllProducts());
        for (DemoProduct demo : CATALOG) {
            Product sameSku = existing.stream().filter(p -> p.getSku().getValue().equals(demo.sku())).findFirst().orElse(null);
            if (sameSku != null) {
                boolean sameIdentity = sameSku.getProductName().getValue().equals(demo.name())
                        && java.util.Objects.equals(sameSku.getBarCode().map(v -> v.getValue()).orElse(null), demo.barcode());
                if (!sameIdentity) throw new IllegalStateException("Conflicto en producto demo con SKU " + demo.sku());
                continue;
            }
            boolean collision = existing.stream().anyMatch(p -> p.getProductName().getValue().equals(demo.name())
                    || (demo.barcode() != null && p.getBarCode().map(v -> v.getValue().equals(demo.barcode())).orElse(false)));
            if (collision) throw new IllegalStateException("Conflicto de nombre o código de barras para " + demo.sku());

            Product product = demo.toProduct();
            StockReceipt receipt = new StockReceipt(supplierByName.get(demo.supplier()).getId());
            StockEntry entry = new StockEntry(receipt.getId(), product.getId(), demo.cost(), demo.stock(), demo.expiration());
            products.registerProduct(product, receipt, entry);
            existing.add(product);
        }
    }

    private Supplier createSupplier(String name) {
        try {
            Supplier supplier = new Supplier(name, null, null);
            suppliers.save(supplier);
            return supplier;
        } catch (DomainException exception) {
            throw new IllegalStateException("Dataset demo de proveedores inválido.", exception);
        }
    }

    private static final List<String> SUPPLIER_NAMES = List.of(
            "Andes Alimentos", "Costa Bebidas", "Sierra Lacteos",
            "Hogar Limpio", "Cuidado Diario", "Mundo Mascotas");

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final List<DemoProduct> CATALOG = List.of(
            d("ARRO-0001-DEMO", "Arroz Extra", "arroz-extra.webp", SaleType.UNIDAD, ProductCategory.ABARROTES_SECOS, "Andes Alimentos", "7750000000001", "4.20", "25", "40", "10", 18),
            d("AZUC-0002-DEMO", "Azucar Rubia", "azucar-rubia.webp", SaleType.GRANEL, ProductCategory.ABARROTES_SECOS, "Andes Alimentos", null, "3.10", "30", "25.500", "5", 18),
            d("ACEI-0003-DEMO", "Aceite Vegetal", "aceite-vegetal.webp", SaleType.UNIDAD, ProductCategory.ABARROTES_SECOS, "Andes Alimentos", "7750000000003", "8.50", "20", "24", "6", 12),
            d("AGUA-0004-DEMO", "Agua Mineral", "agua-mineral.webp", SaleType.UNIDAD, ProductCategory.BEBIDAS, "Costa Bebidas", "7750000000004", "1.20", "40", "60", "12", 24),
            d("CAFE-0005-DEMO", "Cafe Molido", "cafe-molido.webp", SaleType.UNIDAD, ProductCategory.CAFE_INFUSIONES, "Costa Bebidas", "7750000000005", "12.00", "25", "18", "5", 12),
            d("LECH-0006-DEMO", "Leche Entera", "leche-entera.webp", SaleType.UNIDAD, ProductCategory.LACTEOS, "Sierra Lacteos", "7750000000006", "3.40", "20", "30", "8", 6),
            d("DETE-0007-DEMO", "Detergente Floral", "detergente-floral.webp", SaleType.UNIDAD, ProductCategory.LIMPIEZA_HOGAR, "Hogar Limpio", "7750000000007", "6.80", "30", "20", "5", 0),
            d("JABO-0008-DEMO", "Jabon Neutro", "jabon-neutro.webp", SaleType.UNIDAD, ProductCategory.CUIDADO_PERSONAL, "Cuidado Diario", "7750000000008", "2.10", "35", "36", "10", 0),
            d("PANA-0009-DEMO", "Panales Bebe", "panales-bebe.webp", SaleType.UNIDAD, ProductCategory.BEBES, "Cuidado Diario", "7750000000009", "24.00", "20", "12", "4", 0),
            d("GALL-0010-DEMO", "Galletas Vainilla", "galletas-vainilla.webp", SaleType.UNIDAD, ProductCategory.SNACKS_GOLOSINAS, "Costa Bebidas", "7750000000010", "1.80", "35", "48", "12", 10),
            d("ALIM-0011-DEMO", "Alimento Canino", "alimento-canino.webp", SaleType.GRANEL, ProductCategory.MASCOTAS, "Mundo Mascotas", null, "7.50", "25", "35.000", "8", 12));

    private static DemoProduct d(String sku, String name, String asset, SaleType saleType, ProductCategory category,
                                 String supplier, String barcode, String cost, String gain, String stock,
                                 String reorder, int expirationMonths) {
        return new DemoProduct(sku, name, "products/" + asset, saleType, category, supplier, barcode,
                new BigDecimal(cost), new BigDecimal(gain), new BigDecimal(stock), new BigDecimal(reorder),
                expirationMonths == 0 ? null : NOW.plusMonths(expirationMonths));
    }

    private record DemoProduct(String sku, String name, String imagePath, SaleType saleType,
                               ProductCategory category, String supplier, String barcode, BigDecimal cost,
                               BigDecimal gain, BigDecimal stock, BigDecimal reorder, LocalDateTime expiration) {
        Product toProduct() throws DomainException {
            return new Product(sku, name, GainStrategy.PORCENTAJE, gain, reorder, barcode, saleType,
                    stock, category, cost, imagePath);
        }
    }
}
