package org.musicStore.Service;

import org.musicStore.model.Stock;
import java.util.List;
import java.util.stream.Collectors;

public class Filter {
    public List<Stock> filterByCategory(String cat, List<Stock> products) {
        return products.stream()
                .filter(s -> s.getCategory().equalsIgnoreCase(cat))
                .collect(Collectors.toList());
    }

    public List<Stock> filterByPrice(int min, int max, List<Stock> products) {
        if (min < 0 || max < min)
            throw new IllegalArgumentException("Invalid price range.");
        return products.stream()
                .filter(s -> s.getPrice() >= min && s.getPrice() <= max)
                .collect(Collectors.toList());
    }

    public List<Stock> filterByBrand(String brand, List<Stock> products) {
        return products.stream()
                .filter(s -> s.getBrand().equalsIgnoreCase(brand))
                .collect(Collectors.toList());
    }
}
