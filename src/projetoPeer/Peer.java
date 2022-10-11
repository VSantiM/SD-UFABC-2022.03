package projetoPeer;

import jdk.swing.interop.SwingInterOpUtils;
import tcpDemo.ThreadAtendimento;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Peer {



    public static void main(String[] args) {

        String IPAddress_peer, filesDir, IPAddress_peer_relacionado1, IPAddress_peer_relacionado2;
        int port_peer, port_peer_relacionado1, port_peer_relacionado2;
        File pasta_peer;
        ArrayList<String> arquivos_do_peer = new ArrayList<String>();

        //Scanner utilizado para comandos na console
        Scanner sc = new Scanner(System.in);

        String comando;

        while (true) {
            System.out.println("Peer em aguardo do próximo comando:\n-INICIALIZA para inicializar o peer\n-SEARCH para procurar um arquivo na rede");
            comando = sc.nextLine();

            if (comando.equals("INICIALIZA")) {
                // Recebe IP do Peer, Porta do Peer, Pasta de arquivos de arquivos do Peer, IP:Porta Peer1 e IP:Porta Peer2
                // Alterando o delimitador do scanner para separar IP e Porta
                System.out.print("Digite o IP:porta desse Peer: ");

                comando = sc.nextLine();
                IPAddress_peer = comando.split(":")[0];
                port_peer = Integer.parseInt(comando.split(":")[1]);

                // EXEMPLO UTILIZADO C:\Users\PICHAU\Desktop\pasta-exemplo-distribuidos
                System.out.print("Digite a pasta desse Peer: ");
                filesDir = sc.nextLine();

                System.out.print("Digite o IP:Porta do Peer 1: ");
                comando = sc.nextLine();
                IPAddress_peer_relacionado1 = comando.split(":")[0];
                port_peer_relacionado1 = Integer.parseInt(comando.split(":")[1]);

                System.out.print("Digite o IP do Peer 2: ");
                comando = sc.nextLine();
                IPAddress_peer_relacionado2 = comando.split(":")[0];
                port_peer_relacionado2 = Integer.parseInt(comando.split(":")[1]);

                ThreadVerificacaoPasta thread_pasta = new ThreadVerificacaoPasta(filesDir, IPAddress_peer, port_peer);
                thread_pasta.start();

                comando = "";

                // lê todos os arquivos na pasta passado na inicialização e armazena em um ArrayList

            }
            if (comando.equals("SEARCH")){

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
        File pasta_peer = new File(pastaDir);
        ArrayList<String> arquivos_peer_atual = new ArrayList<String>();
        listaArquivosDaPasta(pasta_peer, arquivos_peer_atual);

        System.out.print("arquivos da pasta:");
        for (String arquivos : arquivos_peer_atual) {
            System.out.print(" " + arquivos);
        }
        System.out.println();


        while (true){
            try {
                TimeUnit.SECONDS.sleep(30);
                arquivos_peer_atual = new ArrayList<String>();
                listaArquivosDaPasta(pasta_peer, arquivos_peer_atual);

                System.out.print("Sou " + IP_Peer + ":" + porta_peer + " com arquivos" );
                for (String arquivos : arquivos_peer_atual) {
                    System.out.print(" " + arquivos);
                }
                System.out.println();
//                if (!arquivos_peer_validacao.equals(arquivos_peer_atual)) {
//                    System.out.println("ARQUIVOS MUDARAM");
//                    arquivos_peer_atual = arquivos_peer_validacao;
//                }
            } catch (InterruptedException e) {
                System.out.println("Erro");
            }
        }
    }
}
