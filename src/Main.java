import java.util.*;
import java.util.concurrent.*;

class Order {
    long timeOfCreation;
    int cargoWeight;
    String dest;
    Random rand = new Random();

    public Order() {
        this.timeOfCreation = System.currentTimeMillis();
        this.cargoWeight = rand.nextInt(41) + 10;
        this.dest = rand.nextBoolean() ? "Atlanta" : "Gotham";
    }
}

class Ship {
    static int minCargo = 50;
    static int maxCargo = 300;
    static int orderProfit = 1000;
    static int cancellationSetback = 250;
    static long maxWaitingTime = 60000;

    int cargoWeight;
    int totalTrips;
    Random rand = new Random();
    String dest;

    Ship() {
        this.cargoWeight = 0;
        this.totalTrips =0;
        this.dest = rand.nextBoolean() ? "Atlanta" : "Gotham";
    }

    public void pickUpOrder(Order order) {
        if(Main.totalIncome >= Main.MAX_INCOME)
            return;

        if(System.currentTimeMillis() - order.timeOfCreation > maxWaitingTime) {
            if(order.dest == "Atlanta")
                Main.AtlantaorderDQ.remove(order);
            else
                Main.GothamorderDQ.remove(order);

            System.out.println("Order cancelled");
            synchronized(this) {
                Main.totalIncome -= 250;
                Main.totalCancelled++;
            }
            return;
        }

        cargoWeight += order.cargoWeight;
        System.out.println("Order picked up");

        synchronized(this) {
            Main.totalIncome += orderProfit;
            Main.totalDelivered += 1;
        }

        if(cargoWeight > minCargo && cargoWeight < maxCargo) {
            cargoWeight = 0;
            totalTrips++;

            System.out.println("Shipping orders to: " + dest);

            dest = dest == "Atalanta" ? "Gotham" : "Atlanta";

            if(totalTrips % 5 == 0) {
                System.out.println("Ship under maintanance");

                try {
                    Thread.sleep(60000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class ShippingThread implements Runnable {
    Ship ship;

    public ShippingThread(Ship ship) {
        this.ship = ship;
    }

    @Override
    public void run() {
        while(Main.totalIncome < Main.MAX_INCOME) {
            try {
                if(ship.dest == "Atlanta") {
                    Order order = Main.AtlantaorderDQ.take();
                    ship.pickUpOrder(order);
                } else {
                    Order order = Main.GothamorderDQ.take();
                    ship.pickUpOrder(order);
                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Total orders delivered: " + Main.totalDelivered);
        System.out.println("Total orders cancelled: " + Main.totalCancelled);
    }
}

class ConsumerThread implements Runnable {

    @Override
    public void run() {
        while (Main.totalIncome < Main.MAX_INCOME) {
            try {
                Thread.sleep(5000);
                Order order = new Order();
                if (order.dest == "Atlanta")
                    Main.AtlantaorderDQ.add(order);
                else
                    Main.GothamorderDQ.add(order);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
public class Main {
    static BlockingDeque<Order> AtlantaorderDQ = new LinkedBlockingDeque<>();
    static BlockingDeque<Order> GothamorderDQ = new LinkedBlockingDeque<>();
    static int totalIncome = 0;
    static int totalDelivered = 0;
    static int totalCancelled = 0;
    static final int MAX_INCOME = 1000000;
    static Ship[] ships = new Ship[5];

    public static void main(String[] args) {
        ExecutorService shippingPool = Executors.newFixedThreadPool(5);
        ExecutorService consumerPool = Executors.newFixedThreadPool(7);

        for(int i=0; i<7; i++) {
            consumerPool.submit(new ConsumerThread());
        }

        for(int i=0; i<5; i++) {
            ships[i] = new Ship();
            shippingPool.submit(new ShippingThread(ships[i]));
        }
    }
}
