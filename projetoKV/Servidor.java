package projetoKV;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static projetoKV.Servidor.*;

public class Servidor {
    static String[] SISTEMA = {"127.0.0.1:10097", "127.0.0.1:10098", "127.0.0.1:10099"}; // Lista de todos os IPs:Portas que compôe o sistema
    static int ALL_REPLICATION_OK = 0; // Variável para checar se todos os REPLICATION_OK chegaram
    static boolean isLider = false;
    static HashMap<String, String> Hash_KV = new HashMap<>(); // Hash Key-Value
    static HashMap<String, Long> Hash_KT = new HashMap<String, Long>(); // Hash Key-Timestamp

    // Variáveis de IP e porta do servidor e do líder dele
    static InetAddress IP, IP_lider;
    static int porta, porta_lider; 

    public static void main(String[] args) throws IOException {
        // Inicialização do servidor, solicita IP e porta do servidor e do líder
        Socket no = null; //socket que irá aceitar os requests
        Scanner sc = new Scanner(System.in);
        String comando;

        System.out.print("Digite o IP:Porta do Servidor: ");
        comando = sc.nextLine();
        IP = InetAddress.getByName(comando.split(":")[0]);
        porta = Integer.parseInt(comando.split(":")[1]);

        System.out.print("Digite o IP:Porta do Servidor Líder: ");
        comando = sc.nextLine();
        IP_lider = InetAddress.getByName(comando.split(":")[0]);
        porta_lider = Integer.parseInt(comando.split(":")[1]);

        ServerSocket serverSocket = new ServerSocket(porta);

        if (IP.equals(IP_lider) && porta == porta_lider) { isLider = true;  }

        while (true) {
            // Quando receber uma conexão irá aceitar e mandar para a Thread de processamento
            no = serverSocket.accept();
            ThreadProcessamento tdProcessamento = new ThreadProcessamento(no);
            tdProcessamento.start();
        }
    }
}

// Thread após receber uma conexão, abre o reader para ver a mensagem passada
class ThreadProcessamento extends Thread{
    private Socket no;

    ThreadProcessamento(Socket no) {
        this.no = no;
    }

    public void run(){
        try {
            InputStreamReader is = new InputStreamReader(no.getInputStream());
            BufferedReader reader = new BufferedReader(is);

            // Fica esperando até o reader receber a mensagem
            while (true) {
                // Checa se o reader tem algo
                if (reader.ready()) {
                    String mensagem = reader.readLine();
                    Mensagem msgRecebida = new Mensagem(mensagem);

                    // Abre uma thread para tratar a mensagem
                    ThreadNovaRequisicao tdNewRes = new ThreadNovaRequisicao(msgRecebida);
                    tdNewRes.start();
                    break; // Após ler, sai do loop
                }
            }
        } catch (IOException e) { e.printStackTrace();}
    }
}

class ThreadNovaRequisicao extends Thread {
    private Mensagem msg;

    ThreadNovaRequisicao(Mensagem msg) { this.msg = msg; }

    public void run() {

        // Caso do GET
        if (msg.getTipo_mensagem().equals("GET")) {
            long aux_TSCliente = msg.getTimestamp();
            // Caso a key não exista, retorna null com ts de 0
            if (!Hash_KV.containsKey(msg.getKey())) {
                msg.setValue(null);
                msg.setTimestamp(0);

            } else if (aux_TSCliente > Hash_KT.get(msg.getKey())) {
                // Caso a key exista, porém o TS do Cliente seja maior que o TS do servidor para aquela key
                msg.setValue("TRY_OTHER_SERVER_OR_LATER");
                msg.setTipo_mensagem("TRY_OTHER_SERVER_OR_LATER");
                msg.setTimestamp(Hash_KT.get(msg.getKey()));
            } else {
                // Caso normal do GET, a key existe e o TS do servidor é maior ou igual ao TS do Cliente
                msg.setValue(Hash_KV.get(msg.getKey()));
                msg.setTimestamp(Hash_KT.get(msg.getKey()));
            }

            System.out.println("Cliente " +
                    msg.getIPHostOrigem() +
                    ":" +
                    msg.getPortaHostOrigem() +
                    " GET key:" +
                    msg.getKey() +
                    " ts:" +
                    aux_TSCliente +
                    ". Meu ts é " +
                    Hash_KT.get(msg.getKey())+
                    ", portanto devolvendo " +
                    msg.getValue()
            );

            msg.setIPHostResposta(IP);
            msg.setPortaHostOrigem(porta);
            ThreadComunicacaoCliente tdComunicacaoCliente = new ThreadComunicacaoCliente(msg);
            tdComunicacaoCliente.start();

        } else if (isLider && msg.getTipo_mensagem().equals("PUT")) {
            // Caso que o líder recebe o PUT
            System.out.println("Cliente " +
                    msg.getIPHostOrigem() +
                    ":" +
                    msg.getPortaHostOrigem() +
                    " PUT key:" +
                    msg.getKey() +
                    " value:" +
                    msg.getValue()
            );
            long tsNow = System.currentTimeMillis();

            // Checa se a key já existe e a atualiza
            if (Hash_KV.containsKey(msg.getKey())) {
                Hash_KV.replace(msg.getKey(), msg.getValue());
                Hash_KT.replace(msg.getKey(), tsNow);
            } else {
                // Caso da Key não existir
                Hash_KV.put(msg.getKey(), msg.getValue());
                Hash_KT.put(msg.getKey(), tsNow);
            }

            // Define o timestamp e passa para os demais servidores fazerem a replicação
            msg.setTimestamp(tsNow);
            msg.setTipo_mensagem("REPLICATION");
            ThreadReplication tdReplication = new ThreadReplication(msg);
            tdReplication.start();

        } else if (isLider && msg.getTipo_mensagem().equals("REPLICATION_OK")) {
            ALL_REPLICATION_OK++;
            // CHECA SE TODOS OS SERVIDORES MANDARAM UM REPLICATION OK PARA O LÍDER
            if (ALL_REPLICATION_OK == SISTEMA.length - 1){
                ALL_REPLICATION_OK = 0;
                System.out.println(
                        "Enviando PUT_OK ao Cliente " +
                                msg.getIPHostOrigem() +
                                ":" +
                                msg.getPortaHostOrigem() +
                                " da key:" +
                                msg.getKey() +
                                " ts:" +
                                msg.getTimestamp());

                // Após receber todos os replication OK, envia o PUT_OK para o cliente que solicitou
                msg.setTipo_mensagem("PUT_OK");
                msg.setIPHostResposta(IP);
                msg.setPortaHostOrigem(porta);

                // Inicia a thread de comunicação com o cliente
                ThreadComunicacaoCliente tdComunicacaoCliente = new ThreadComunicacaoCliente(msg);
                tdComunicacaoCliente.start();
            }
        } else if (!isLider && msg.getTipo_mensagem().equals("PUT")) {
            // Caso recebe um PUT e não seja o líder, encaminha para o líder
            System.out.println("Encaminhando PUT key:" + msg.getKey() + " value:" + msg.getValue());

            // Thread para comunicação com o líder passando a mensagem recebida
            ThreadComunicacaoLider tdEnviaLider = new ThreadComunicacaoLider(msg);
            tdEnviaLider.start();
        } else if (!isLider && msg.getTipo_mensagem().equals("REPLICATION")) {
            System.out.println(
                    "REPLICATION key:" +
                            msg.getKey() +
                            " value:" +
                            msg.getValue() +
                            " ts:" +
                            msg.getTimestamp()
            );
            // Checa se a key já existe e a atualiza
            if (Hash_KV.containsKey(msg.getKey())) {
                Hash_KV.replace(msg.getKey(), msg.getValue());
                Hash_KT.replace(msg.getKey(), msg.getTimestamp());
            } else {
                // Caso da Key não existir
                Hash_KV.put(msg.getKey(), msg.getValue());
                Hash_KT.put(msg.getKey(), msg.getTimestamp());
            }
            System.out.println("Enviando REPLICATION_OK para o líder");
            msg.setTipo_mensagem("REPLICATION_OK");
            ThreadComunicacaoLider tdEnviaLider = new ThreadComunicacaoLider(msg);
            tdEnviaLider.start();
        }
    }
}

class ThreadComunicacaoLider extends Thread {
    Mensagem msg;

    ThreadComunicacaoLider(Mensagem msg) { this.msg = msg; }

    public void run() {
        try {
            Socket s = new Socket(IP_lider, porta_lider);
            DataOutputStream writer = new DataOutputStream(s.getOutputStream());

            writer.writeBytes(msg.toString());
            s.close();
        } catch (IOException e) {}
    }
}

class ThreadComunicacaoCliente extends Thread {
    Mensagem msg;

    ThreadComunicacaoCliente(Mensagem msg) { this.msg = msg; }

    public void run(){
        try {
            Socket s = new Socket(msg.getIPHostOrigem().getHostAddress(), msg.getPortaHostOrigem());
            DataOutputStream writer = new DataOutputStream(s.getOutputStream());

            writer.writeBytes(msg.toString());
            s.close();
        } catch (IOException e) {}


    }
}

class ThreadReplication extends Thread {
    private Mensagem msgParaReplicar;

    ThreadReplication(Mensagem msg) {
        this.msgParaReplicar = msg;
    }

    public void run() {
        for (String servidor : SISTEMA) {
            // Não replica para o líder, pois apenas ele chama o método
            if (servidor.split(":")[0].equals(IP_lider) && Integer.parseInt(servidor.split(":")[1]) == porta_lider);
            else {
                try {
                    //TimeUnit.SECONDS.sleep(10); // APENAS PARA SIMULAR O ATRASO PARA INCONSISTÊNCIAS. NA VERSÃO FINAL RETIRAR ESSA LINHA
                    Socket s = new Socket(servidor.split(":")[0], Integer.parseInt(servidor.split(":")[1]));
                    OutputStream os = s.getOutputStream();
                    DataOutputStream writer = new DataOutputStream(os);
                    writer.writeBytes( msgParaReplicar.toString());
                    s.close();
                } catch (Exception e) {}
            }
        }
    }
}