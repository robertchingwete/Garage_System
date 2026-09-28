package models;



public class Part {
    private String id;
    private String name;
    private String category;
    private double price;
    private int stock;
    private String status;
    private String description;
    private String imagePath;
    private int minStockLevel;

    public Part(String id, String name, String category, double price, int stock, String status, String description, String imagePath) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.description = description;
        this.imagePath = imagePath;
        this.minStockLevel = 10; // Default minimum stock level
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) {
        this.stock = stock;
        updateStatus();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(int minStockLevel) { this.minStockLevel = minStockLevel; }

    // Auto-update status based on stock level
    private void updateStatus() {
        if (stock <= 0) {
            status = "Out of Stock";
        } else if (stock <= minStockLevel) {
            status = "Low Stock";
        } else {
            status = "In Stock";
        }
    }

    // Calculate stock level percentage
    public double getStockPercentage() {
        int maxStock = Math.max(stock, minStockLevel * 3); // Assume max is 3x minimum
        return Math.min(1.0, (double) stock / maxStock);
    }

    // Check if stock is low
    public boolean isLowStock() {
        return stock <= minStockLevel && stock > 0;
    }

    // Check if out of stock
    public boolean isOutOfStock() {
        return stock <= 0;
    }
}
