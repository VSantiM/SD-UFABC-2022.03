package projetoPeer;


import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static projetoPeer.Peer.*;

public class Peer {
    // Socket do Peer para comunicação
    static DatagramSocket serverSocket;

    // TTL para timeout da procura de arquivo, aqui está com 30 reenvios antes de retornar o timeout para o Peer
    private static final int TTL = 30;
    static final int TEMPO_TIMEOUT_SEGUNDOS = 30;

    // Hasmap para armazenar os arquivos já procurados e as requisições que já sofreram timeout com o ID da sua requisição
    static HashMap arquivos_ja_procurados = new HashMap();
    static HashMap requisicoes_respondidas = new HashMap();

    // ArrayList para armazenar os arquivos da pasta do Peer
    static ArrayList<String> arquivos_peer_atual = new ArrayList<>();

    // Variáveis do Peer: IP:Porta, pasta, e IP:Porta dos Peers relacionados
    static String filesDir;
    static int port_peer, port_peer_relacionado1, port_peer_relacionado2;
    static InetAddress hostPeer, hostPeerRelacionado1, hostPeerRelacionado2;

    //UUID para gerar sequências únicas das requisições de busca
    static UUID uuid;

    public static void main(String[] args) throws Exception {
        //Scanner utilizado para comandos na console
        Scanner sc = new Scanner(System.in);
        String comando, nome_arquivo_procurado;

        while (true) {
            System.out.println("Peer em aguardo do próximo comando:\n-INICIALIZA para inicializar o peer\n-SEARCH para procurar um arquivo na rede");
            comando = sc.nextLine();


            if (comando.equals("INICIALIZA")) {
                // Recebe IP do Peer, Porta do Peer, Pasta de arquivos de arquivos do Peer, IP:Porta Peer1 e IP:Porta Peer2
                System.out.print("Digite o IP:porta desse Peer: ");

                comando = sc.nextLine();
                hostPeer = InetAddress.getByName(comando.split(":")[0]);
                port_peer = Integer.parseInt(comando.split(":")[1]);

                // Input de qual pasta o Peer irá ficar olhando
                System.out.print("Digite a pasta desse Peer: ");
                filesDir = sc.nextLine();

                System.out.print("Digite o IP:Porta do Peer 1: ");
                comando = sc.nextLine();
                hostPeerRelacionado1 = InetAddress.getByName(comando.split(":")[0]);
                port_peer_relacionado1 = Integer.parseInt(comando.split(":")[1]);

                System.out.print("Digite o IP do Peer 2: ");
                comando = sc.nextLine();
                hostPeerRelacionado2 = InetAddress.getByName(comando.split(":")[0]);
                port_peer_relacionado2 = Integer.parseInt(comando.split(":")[1]);

                ThreadVerificacaoPasta thread_pasta = new ThreadVerificacaoPasta(filesDir, hostPeer.getHostAddress(), port_peer);
                thread_pasta.start();

                comando = "";

                // Cria socket do Peer (Como se ele fosse um servidor), conecta com os dois peers e inicia a thread para ficar escutando
                serverSocket = new DatagramSocket(port_peer, hostPeer);

                // Chamada da Thread que irá escutar a porta do Peer para o recebimento de mensagens
                ThreadListenSocket threadListenSocket = new ThreadListenSocket(serverSocket);
                threadListenSocket.start();
            }

            if (comando.equals("SEARCH")){
                System.out.print("Nome do arquivo procurado: ");
                nome_arquivo_procurado = sc.nextLine();

                // Cria um identificador e adiciona no HashMap de arquivos procurados
                uuid = UUID.randomUUID();

                // Cria uma classe Mensagem e inicia a Thread que irá fazer a busca do arquivo
                Mensagem msgBusca = new Mensagem("SEARCH", uuid, nome_arquivo_procurado, hostPeer, port_peer, TTL);
                ThreadProcuraArquivo threadBusca = new ThreadProcuraArquivo(msgBusca);
                threadBusca.start();

                ThreadTimeoutPorTempo threadTimeoutPorTempo = new ThreadTimeoutPorTempo(msgBusca);
                threadTimeoutPorTempo.start();
            }
        }
    }
}

class ThreadVerificacaoPasta extends Thread {
    private String pastaDir, IP_Peer;
    private int porta_peer;

    ThreadVerificacaoPasta(String pasta, String IP_Peer, int porta_peer) {
        pastaDir = pasta;
        this.IP_Peer = IP_Peer;
        this.porta_peer = porta_peer;
    }

    public static void listaArquivosDaPasta(final File pasta, ArrayList<String> arquivos_peer) {
        // Função para armazenar todos os arquivos da pasta em uma ArrayList
        ArrayList<String> arquivos_do_peer = new ArrayList<String>();

        for (final File arquivo : pasta.listFiles()) {
            // Checa se o arquivo é uma pasta também para checar os arquivos dela
            if (arquivo.isDirectory()) {
                listaArquivosDaPasta(arquivo, arquivos_peer);
            } else {
                arquivos_peer.add(arquivo.getName());
            }
        }
    }

    public void run() {
       //Delay de 100 ms para os prints não conflitarem no menu
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {}
        File pasta_peer = new File(pastaDir);
        listaArquivosDaPasta(pasta_peer, arquivos_peer_atual);

        System.out.print("arquivos da pasta:");
        for (String arquivos : arquivos_peer_atual) {
            System.out.print(" " + arquivos);
        }
        System.out.println();


        while (true){
            try {
                TimeUnit.SECONDS.sleep(30);
                arquivos_peer_atual = new ArrayList<>();
                listaArquivosDaPasta(pasta_peer, arquivos_peer_atual);

                System.out.print("Sou " + IP_Peer + ":" + porta_peer + " com arquivos" );
                for (String arquivos : arquivos_peer_atual) {
                    System.out.print(" " + arquivos);
                }
                System.out.println();

            } catch (InterruptedException e) {
                System.out.println(e.getStackTrace());
            }
        }
    }
}

class ThreadProcuraArquivo extends Thread {
    private Mensagem msgBuscada;
    DatagramPacket sendPacket = null;

    public ThreadProcuraArquivo(Mensagem msgBusca) {
        this.msgBuscada = msgBusca;
    }

    public void run() {
        // Checa se essa requisição já foi processada - caso sim é ignorado
        if(!arquivos_ja_procurados.containsKey(msgBuscada.getUUIDRequisicao())) {
            arquivos_ja_procurados.put(msgBuscada.getUUIDRequisicao(), msgBuscada.getNomeArquivoProcurado());

            if(arquivos_peer_atual.contains(msgBuscada.getNomeArquivoProcurado()) && msgBuscada.getTTLReq() > 0) {
                // Caso o peer atual tenha o arquivo, irá gerar uma mensagem RESPONSE para o peer solicitante
                System.out.println(
                        "tenho " +
                        msgBuscada.getNomeArquivoProcurado() +
                        " respondendo para " +
                        msgBuscada.getIPHostOrigem() +
                        ":" +
                        msgBuscada.getIPHostOrigem());
                msgBuscada.setTipo_mensagem("RESPONSE");
                try {
                    sendPacket = new DatagramPacket(
                            msgBuscada.toString().getBytes(StandardCharsets.UTF_8),
                            (msgBuscada.toString().getBytes(StandardCharsets.UTF_8).length),
                            msgBuscada.getIPHostOrigem(),
                            msgBuscada.getPortaHostOrigem());
                    serverSocket.send(sendPacket);
                } catch (Exception e) {}
            } else if (msgBuscada.getTTLReq() > 0) {
                // Caso que o Peer não tem o arquivo buscado manda a busca para um próximo Peer caso o TTL da mensagem ainda seja maior que 0
                String proximoHostEPorta = (Math.random() <= 0.5) ?
                        (hostPeerRelacionado1.getHostAddress() + ":" + port_peer_relacionado1) :
                        (hostPeerRelacionado2.getHostAddress() + ":" + port_peer_relacionado2);
                System.out.println(
                        "não tenho " +
                        msgBuscada.getNomeArquivoProcurado() +
                        " encaminhando para " +
                        proximoHostEPorta);
                msgBuscada.reenviarMensagem(); // reduz o TTL
                DatagramPacket sendPacket = null;
                try {
                    sendPacket = new DatagramPacket(
                            msgBuscada.toString().getBytes(StandardCharsets.UTF_8),
                            (msgBuscada.toString().getBytes(StandardCharsets.UTF_8).length),
                            InetAddress.getByName(proximoHostEPorta.split(":")[0]),
                            Integer.parseInt(proximoHostEPorta.split(":")[1]));
                    serverSocket.send(sendPacket);
                } catch (Exception e) {}
            } else {
                // CASO EM QUE O TTL DA MENSAGEM CHEGOU EM ZERO RESPONDE AO HOST SOLICITANTE COM UM TIMEOUT
                msgBuscada.setTipo_mensagem("TIMEOUT");
                DatagramPacket sendPacket = new DatagramPacket(
                        msgBuscada.toString().getBytes(StandardCharsets.UTF_8),
                        (msgBuscada.toString().getBytes(StandardCharsets.UTF_8).length),
                        msgBuscada.getIPHostOrigem(),
                        msgBuscada.getPortaHostOrigem());
                try {
                    serverSocket.send(sendPacket);
                } catch (IOException e) {}
            }
        } else {
            System.out.println("requisição já processada para " + msgBuscada.getNomeArquivoProcurado());
        }
    }
}

class ThreadListenSocket extends Thread {
    // Thread para ficar ouvindo o socket e enviar os pacotes recebidos para tratamento
    private DatagramSocket socket;
    byte[] recBuffer;
    private DatagramPacket recPkt;
    String informacao;
    Mensagem mensagemRecebida;

    ThreadListenSocket(DatagramSocket serverSocket) {this.socket = serverSocket;}

    public void run(){
        try {

            while (true) {

                recBuffer = new byte[40960];
                recPkt = new DatagramPacket(recBuffer, recBuffer.length);
                socket.receive(recPkt); // Recebe a requisição

                // Thread para tratamento do package recebido
                ThreadTratamentoRecebido threadTratamentoRecebido = new ThreadTratamentoRecebido(recPkt);
                threadTratamentoRecebido.start();
            }
        } catch (Exception e) {
            // Caso aconteça algo com a thread que fica ouvindo o socket, outra é criada.
            ThreadListenSocket repeatThread = new ThreadListenSocket(this.socket);
            repeatThread.start();
        }
    }
}

class ThreadTratamentoRecebido extends Thread{
    private DatagramPacket packageRecebido;
    private Mensagem mensagemRecebida;

    // Recebe um pacote com a classe Mensagem passada para String
    public ThreadTratamentoRecebido(DatagramPacket packet) {
        this.packageRecebido = packet;
    }

    public void run() {
        String informacao = new String (
                packageRecebido.getData(),
                packageRecebido.getOffset(),
                packageRecebido.getLength()
        );

        // Parser da informação recebida para um objeto mensagem
        try {
           this.mensagemRecebida = new Mensagem(informacao);
        } catch (Exception e) { e.printStackTrace(); }

        // De acordo com o tipo da Mensagem recebida, enderaça para outra thread de busca ou armazena a resposta recebida
        if (mensagemRecebida.getTipo_mensagem().equals("SEARCH")) {
            ThreadProcuraArquivo procuraArquivo = new ThreadProcuraArquivo(mensagemRecebida);
            procuraArquivo.start();
        } else if (mensagemRecebida.getTipo_mensagem().equals("RESPONSE") && !requisicoes_respondidas.containsKey(mensagemRecebida.getUUIDRequisicao())) {
            requisicoes_respondidas.put(mensagemRecebida.getUUIDRequisicao(), mensagemRecebida.getNomeArquivoProcurado());
            System.out.println("peer com arquivo procurado: " + packageRecebido.getAddress() + ":" + packageRecebido.getPort() + " " + mensagemRecebida.getNomeArquivoProcurado());
        } else if (mensagemRecebida.getTipo_mensagem().equals("TIMEOUT") && !requisicoes_respondidas.containsKey(mensagemRecebida.getUUIDRequisicao())) {
            requisicoes_respondidas.put(mensagemRecebida.getUUIDRequisicao(), mensagemRecebida.getNomeArquivoProcurado());
            System.out.println("ninguém no sistema possui o arquivo " + mensagemRecebida.getNomeArquivoProcurado());
        }
    }
}

class ThreadTimeoutPorTempo extends Thread {
    private Mensagem msgArquivoBuscado;

    // Classe gerada para criar um timeout por tempo caso a busca não atinja o TTL minímo.
    public ThreadTimeoutPorTempo(Mensagem msgBusca) {
        this.msgArquivoBuscado = msgBusca;
    }

    public void run() {
        try {
            TimeUnit.SECONDS.sleep(TEMPO_TIMEOUT_SEGUNDOS);

            // APÓS O TIMEOUT EM TEMPO DEFINIDO, RESPONDE AO HOST SOLICITANTE UMA MENSAGEM DE TIMEOUT
            msgArquivoBuscado.setTipo_mensagem("TIMEOUT");
            DatagramPacket sendPacket = new DatagramPacket(
                    msgArquivoBuscado.toString().getBytes(StandardCharsets.UTF_8),
                    (msgArquivoBuscado.toString().getBytes(StandardCharsets.UTF_8).length),
                    msgArquivoBuscado.getIPHostOrigem(),
                    msgArquivoBuscado.getPortaHostOrigem());
            try {
                serverSocket.send(sendPacket);
            } catch (IOException e) {}
        } catch (InterruptedException e) {}

    }

}
