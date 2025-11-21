package com.oop.logistics.preprocessing;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts location information from text
 */
public class LocationExtractor {
    
    private final Set<String> vietnameseProvinces;
    private final Set<String> vietnameseCities;
    private final Pattern locationPattern;
    
    public LocationExtractor() {
        this.vietnameseProvinces = initializeProvinces();
        this.vietnameseCities = initializeCities();
        
        // Pattern to match location phrases
        this.locationPattern = Pattern.compile(
            "(tại|ở|tỉnh|thành phố|huyện|xã|tp\\.|TP\\.)\\s+([\\p{L}\\s]+?)(?=[,\\.;]|$)",
            Pattern.CASE_INSENSITIVE
        );
    }
    
    private Set<String> initializeProvinces() {
        return new HashSet<>(Arrays.asList(
            "Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
            "An Giang", "Bà Rịa-Vũng Tàu", "Bắc Giang", "Bắc Kạn", "Bạc Liêu",
            "Bắc Ninh", "Bến Tre", "Bình Định", "Bình Dương", "Bình Phước",
            "Bình Thuận", "Cà Mau", "Cao Bằng", "Đắk Lắk", "Đắk Nông",
            "Điện Biên", "Đồng Nai", "Đồng Tháp", "Gia Lai", "Hà Giang",
            "Hà Nam", "Hà Tĩnh", "Hải Dương", "Hậu Giang", "Hòa Bình",
            "Hưng Yên", "Khánh Hòa", "Kiên Giang", "Kon Tum", "Lai Châu",
            "Lâm Đồng", "Lạng Sơn", "Lào Cai", "Long An", "Nam Định",
            "Nghệ An", "Ninh Bình", "Ninh Thuận", "Phú Thọ", "Phú Yên",
            "Quảng Bình", "Quảng Nam", "Quảng Ngãi", "Quảng Ninh", "Quảng Trị",
            "Sóc Trăng", "Sơn La", "Tây Ninh", "Thái Bình", "Thái Nguyên",
            "Thanh Hóa", "Thừa Thiên Huế", "Tiền Giang", "Trà Vinh", "Tuyên Quang",
            "Vĩnh Long", "Vĩnh Phúc", "Yên Bái"
        ));
    }
    
    private Set<String> initializeCities() {
        return new HashSet<>(Arrays.asList(
            "Hanoi", "Ho Chi Minh", "Da Nang", "Hai Phong", "Can Tho",
            "Hue", "Nha Trang", "Buon Ma Thuot", "Vung Tau", "Bien Hoa"
        ));
    }
    
    /**
     * Extract location from text description
     */
    public String extractLocation(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        // First try to find Vietnamese provinces
        for (String province : vietnameseProvinces) {
            if (text.contains(province)) {
                return province;
            }
        }
        
        // Try to find cities
        for (String city : vietnameseCities) {
            if (text.toLowerCase().contains(city.toLowerCase())) {
                return city;
            }
        }
        
        // Use pattern matching for location phrases
        Matcher matcher = locationPattern.matcher(text);
        if (matcher.find()) {
            String location = matcher.group(2).trim();
            if (location.length() > 3 && location.length() < 50) {
                return capitalizeWords(location);
            }
        }
        
        return null;
    }
    
    /**
     * Extract all possible locations from text
     */
    public List<String> extractAllLocations(String text) {
        List<String> locations = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return locations;
        }
        
        // Find all provinces
        for (String province : vietnameseProvinces) {
            if (text.contains(province)) {
                locations.add(province);
            }
        }
        
        // Find all cities
        for (String city : vietnameseCities) {
            if (text.toLowerCase().contains(city.toLowerCase())) {
                locations.add(city);
            }
        }
        
        return locations;
    }
    
    /**
     * Add custom location to the recognizer
     */
    public void addProvince(String province) {
        vietnameseProvinces.add(province);
    }
    
    public void addCity(String city) {
        vietnameseCities.add(city);
    }
    
    private String capitalizeWords(String text) {
        String[] words = text.split("\\s+");
        StringBuilder capitalized = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1).toLowerCase())
                          .append(" ");
            }
        }
        
        return capitalized.toString().trim();
    }
}