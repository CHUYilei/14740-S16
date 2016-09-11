/**
 * For testing dynamic CGI
 */
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CalculateSum {
    public static double getSum(double a,double b){
        return a+b;
    }

    public static void printToFile(double a,double b){
        try {
            PrintWriter writer = new PrintWriter("../www/cgi-bin/temp.txt", "UTF-8");
            writer.println("a: "+a+" b: "+b);
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            writer.println("current time: "+sdf.format(cal.getTime()));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        double a = 0.0,b = 0.0;

        /* Parse parameter and do args checking */
        if (args.length != 2) {
            System.err.println("args.length: "+args.length);
            System.err.println("Usage: java CalculateSum <Addend_a> <Addend_b>");
            System.exit(1);
        }

        try {
            a = Double.parseDouble(args[0]);
            b = Double.parseDouble(args[1]);
        } catch (Exception e) {
            System.err.println("I/O exception");
            System.err.println("Usage: java CalculateSum <Addend_a> <Addend_b>");
            System.exit(1);
        }

        printToFile(a,b);

        System.out.println("Server performs dynamic cgi calculation, result is "+CalculateSum.getSum(a,b));
    }
}
