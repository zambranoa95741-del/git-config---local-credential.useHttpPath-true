public class Motorcycle extends Vehicle {
    private boolean hasSidecar;

    public Motorcycle(String brand, int year, boolean hasSidecar) {
        super(brand, year);
        this.hasSidecar = hasSidecar;
    }

    @Override
    public void displayInfo() {
        if (hasSidecar) {
            System.out.println("Motorcycle: " + brand + " (" + year + ") - with sidecar");
        } else {
            System.out.println("Motorcycle: " + brand + " (" + year + ") - no sidecar");
        }
    }
}
