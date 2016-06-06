package bank;

public class Server {    
    private static final String USAGE = "java bankjdbc.Server [rmi-URL of a bank] " + "[database] [dbms: access, derby, pointbase, cloudscape, mysql]";
    private static final String BANK = "BANKS.Nordea";
    private static final String DATASOURCE = "BANKS";
    private static final String DBMS = "derby";

    public Server(String bankName, String ds, String dbms) {
            try {
                    Bank bankobj = new BankImpl(bankName, ds, dbms);
                    // Register the newly created object at rmiregistry.
                    java.rmi.Naming.rebind(bankName, bankobj);
                    System.out.println(bankobj + " is ready.");
            } catch (Exception e) {
                    System.out.println(e);
            }
    }

    public static void main(String[] args) {
            if (args.length > 3 || (args.length > 0 && args[0].equalsIgnoreCase("-h"))) {
                    System.out.println(USAGE);
                    System.exit(1);
            }

            String bankName = null;
            if (args.length > 0) {
                    bankName = args[0];
            } else {
                    bankName = BANK;
            }

            String ds = null;
            if (args.length > 1) {
                    ds = args[1];
            } else {
                    ds = DATASOURCE;
            }

            String dbms = null;
            if (args.length > 2) {
                    dbms = args[2];
            } else {
                    dbms = DBMS;
            }
            new Server(bankName, ds, dbms);
    }
}
