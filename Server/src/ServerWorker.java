import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ServerWorker extends Thread{

    private final Socket clientSocket;
    private final Server server;
    private String login = null;
    private OutputStream out;
    private InputStream in;


    public ServerWorker( Server server, Socket clientSocket ) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    public void handleClientSocket(Socket clientSocket) {
        try{
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();

            BufferedReader reader = new BufferedReader( new InputStreamReader( in ));
            String input, cmd;

            while ( (input = reader.readLine()) != null) {
                String[] tokens = input.split( " " );
                if ( tokens.length > 0 ) {
                    cmd = tokens[0];
                    if ( "quit".equalsIgnoreCase( cmd ) || "logoff".equalsIgnoreCase( cmd ) ) {
                        handleLogoff();
                        break;
                    }else if ("login".equalsIgnoreCase( cmd )) {
                        handleLogin( out, tokens );
                    }else if ("msg".equalsIgnoreCase( cmd )) {
                        String[] tokensMsg = input.split( " ", 3 );
                        handleMessage( tokensMsg );
                    }
                }
            }
            clientSocket.close();
        } catch ( IOException e) {
            e.printStackTrace();
        }
    }

    // format: "msg" "login" message...
    public void handleMessage( String[] tokens ) throws IOException {
        String sendTo = tokens[1], body_msg = tokens[2];
        List<ServerWorker> workList = server.getWorkerList();
        for ( ServerWorker worker : workList ){
            try {
                if ( sendTo.equalsIgnoreCase( worker.getLogin() ) ) {
                    String out_msg = "msg " + login + " " + body_msg + "\n";
                    worker.send( out_msg.getBytes() );
                }
            }catch ( SocketException err ){
            }
        }

    }

    public void handleLogoff() throws IOException {
        server.removeWorker(this);
        List<ServerWorker> workList = server.getWorkerList();

        for(ServerWorker worker : workList) {
            if ( !login.equals( worker.getLogin() ) ) {
                worker.send( ("Offline: " + login + "\n").getBytes() );
            }
        }
        clientSocket.close();
    }

    public String getLogin(){ return login; }

    public void handleLogin( OutputStream out, String[] tokens ) throws IOException {
        if(tokens.length == 2){
            String login = tokens[1];

            if(!login.isBlank()){
                List<ServerWorker> workList = server.getWorkerList();


                // Send current user all others online status

                for(ServerWorker worker : workList) {
                    if (worker.getLogin() != null && !login.equals( worker.getLogin()) ){
                        String message = "\nonline: " + worker.getLogin() + "\n";
                        send( message.getBytes());

                    }
                }

                //send other online users current user's status
                String online_Status = "\nOnline: " + login + "\n";
                for(ServerWorker worker : workList) {
                    if ( !login.equals( worker.getLogin() ) ) {
                        worker.send(online_Status.getBytes());
                    }
                }
            }else{
                out.write( ("Login unknown: " + login + "\n").getBytes() );
                System.err.println("Login failed: <" + login + ">");

            }
        }
    }

    public void send( byte[] msg ) throws IOException{
        if (login != null){
            out.write( msg );
        }
    }

}
