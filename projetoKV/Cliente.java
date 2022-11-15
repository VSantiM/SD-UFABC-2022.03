package projetoKV;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import static projetoKV.Cliente.*;

public class Cliente {
    static ArrayList<String> lista_Servidores = new ArrayList<>(); // Lista de servidores conectados
    static HashMap<String, String> Hash_KV_Cliente = new HashMap<>(); // HashTable Key-Value
    static HashMap<String, Long> Hash_KT_Cliente = new HashMap<>(); // HashTable Key-Timestamp


    public static void main(String[] args) throws IOException {
        // Socket do cliente para receber as responses, passado o valor 0 pro construtor abrir em uma porta disponível
        final ServerSocket socketCliente = new ServerSocket(0);

        // Thread para ficar ouvindo o socket do cliente
        ThreadSocketCliente tdSocket = new ThreadSocketCliente(socketCliente);
        tdSocket.start();

        //Scanner utilizado para comandos na console
        Scanner sc = new Scanner(System.in);
        String comando, key_temp, value_temp;

        while (true) {
            // ÍNICIO DO MENU INTERATIVO
            System.out.println("Cliente em aguardo do próximo comando:\n-INIT para inicializar\n-PUT para incluir um KV\n-GET para encontrar um Value com base na Key");
            comando = sc.nextLine();

            if(comando.equals("INIT")) {
                // Inicialização do Cliente e recebimento dos 3 IPs:Porta dos Servidores
                // Cada um irá criar uma Thread para conexão
                System.out.print("Digite o IP:Porta do Servidor 1: ");
                lista_Servidores.add(sc.nextLine());
                System.out.print("Digite o IP:Porta do Servidor 2: ");
                lista_Servidores.add(sc.nextLine());
                System.out.print("Digite o IP:Porta do Servidor 3: ");
                lista_Servidores.add(sc.nextLine());
            } else if (comando.equals("PUT")) {
                // Caso do PUT: Recebe a key e o value em seguida
                System.out.print("Digita o Key a ser inserido: ");
                key_temp = sc.nextLine();
                System.out.print("Digita o Value a ser inserido: ");
                value_temp = sc.nextLine();

                // Cria uma mensagem de PUT e usa a Thread de envio para mandar para o servidor
                Mensagem msPut = new Mensagem("PUT", InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()), socketCliente.getLocalPort(), InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()), socketCliente.getLocalPort(), key_temp, value_temp);
                ThreadEnvio tdPut = new ThreadEnvio(msPut);
                tdPut.start();
            } else if (comando.equals("GET")) {
                // Caso do GET: Solicita a key a ser procurada
                System.out.print("Digita o Key a ser procurado: ");
                key_temp = sc.nextLine();

                long timestampVisto = 0;
                if (Hash_KT_Cliente.containsKey(key_temp)) {
                    timestampVisto = Hash_KT_Cliente.get(key_temp);
                }

                Mensagem mgGet = new Mensagem("GET", InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()), socketCliente.getLocalPort(), InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()), socketCliente.getLocalPort(), key_temp, timestampVisto);
                ThreadEnvio tdGet = new ThreadEnvio(mgGet);
                tdGet.start();
            }
        }
    }
}

class ThreadSocketCliente extends Thread {
    private ServerSocket socketCliente;

    ThreadSocketCliente(ServerSocket socketCliente) {this.socketCliente = socketCliente; }

    public void run(){
        // Socket para receber responses direcionadas
        try {
            while (true) {
                // Quando receber uma conexão irá aceitar e mandar para a Thread de processamento
                Socket no = socketCliente.accept();
                ThreadProcessamentoCliente tdProcessamento = new ThreadProcessamentoCliente(no);
                tdProcessamento.start();
            }

        } catch (IOException e) {}
    }
}

class ThreadProcessamentoCliente extends Thread{
    private Socket no;

    ThreadProcessamentoCliente(Socket no) {
        this.no = no;
    }

    public void run(){
        try {
            InputStreamReader is = new InputStreamReader(no.getInputStream());
            BufferedReader reader = new BufferedReader(is);

            String mensagem = reader.readLine();
            Mensagem msgRecebida = new Mensagem(mensagem);

            if (msgRecebida.getTipo_mensagem().equals("PUT_OK")) {
                Hash_KV_Cliente.put(msgRecebida.getKey(), msgRecebida.getValue());
                Hash_KT_Cliente.put(msgRecebida.getKey(), msgRecebida.getTimestamp());

                System.out.println(
                        "PUT_OK key: " + msgRecebida.getKey() +
                        " value: " + msgRecebida.getValue() +
                        " obtido do servidor " + msgRecebida.getIPHostResposta() + ":" + msgRecebida.getPortaHostResposta()
                );
            } else if (msgRecebida.getTipo_mensagem().equals("GET")) {
                Hash_KV_Cliente.put(msgRecebida.getKey(), msgRecebida.getValue());
                Hash_KT_Cliente.put(msgRecebida.getKey(), msgRecebida.getTimestamp());

                System.out.println(
                        "GET key: " + msgRecebida.getKey() +
                        " value: " + msgRecebida.getValue() +
                        " obtido do servidor " + msgRecebida.getIPHostResposta() + ":" + msgRecebida.getPortaHostResposta() +
                        ", meu timestamp " +  Hash_KT_Cliente.get(msgRecebida.getKey()) +
                        " e do servidor " + msgRecebida.getTimestamp()
                );
            } else if (msgRecebida.getTipo_mensagem().equals(("TRY_OTHER_SERVER_OR_LATER"))) {
                System.out.println(
                        "GET key: " + msgRecebida.getKey() +
                                " value: " + msgRecebida.getValue() +
                                " obtido do servidor " + msgRecebida.getIPHostResposta() + ":" + msgRecebida.getPortaHostResposta() +
                                ", meu timestamp " +  Hash_KT_Cliente.get(msgRecebida.getKey()) +
                                " e do servidor " + msgRecebida.getTimestamp()
                );
            } else {
                System.out.println("Deu algum erro aqui");
                System.out.println(msgRecebida.toString());
            }
        } catch (IOException e) {}
    }
}


class ThreadEnvio extends Thread {
    private Mensagem msg;

    ThreadEnvio(Mensagem msg) {
        this.msg = msg;
    }

    public void run(){
        // Pegar um servidor aleatório
        if (!lista_Servidores.isEmpty()) {
            int index_rand = (int)(Math.random() * lista_Servidores.size());

            Socket s_escolhido = null;
            OutputStream os = null;

            try {
                s_escolhido = new Socket(
                        lista_Servidores.get(index_rand).split(":")[0],
                        Integer.parseInt(lista_Servidores.get(index_rand).split(":")[1])
                );
                os = s_escolhido.getOutputStream();
                DataOutputStream writer = new DataOutputStream(os);
                writer.writeBytes( msg.toString());
                s_escolhido.close();
            } catch (IOException e) {}
        }
    }
}
