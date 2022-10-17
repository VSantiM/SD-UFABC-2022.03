package projetoPeer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class Mensagem {
    private String tipo_mensagem;
    private String nomeArquivoProcurado;
    private UUID UUIDRequisicao;
    private InetAddress IPHostOrigem;
    private int portaHostOrigem;
    private int TTLReq;

    public Mensagem(String tipo_mensagem, UUID UUIDRequisicao, String nomeArquivoProcurado, InetAddress IPHostOrigem, int portaHostOrigem, int TTLReq) {
        this.tipo_mensagem = tipo_mensagem;
        this.IPHostOrigem = IPHostOrigem;
        this.portaHostOrigem = portaHostOrigem;
        this.UUIDRequisicao = UUIDRequisicao;
        this.nomeArquivoProcurado = nomeArquivoProcurado;
        this.TTLReq = TTLReq;
    }

    public Mensagem(String informacao) throws UnknownHostException {
        //Parser
        this.tipo_mensagem = informacao.substring(informacao.indexOf("TIPO:[") + 6, informacao.indexOf("]", informacao.indexOf("TIPO:[") + 1));
        this.nomeArquivoProcurado = informacao.substring(informacao.indexOf("NOME_ARQ:[") + 10, informacao.indexOf("]", informacao.indexOf("NOME_ARQ:[") + 1));
        this.UUIDRequisicao = UUID.fromString(informacao.substring(informacao.indexOf("UUID_REQ:[") + 10, informacao.indexOf("]", informacao.indexOf("UUID_REQ:[") + 1)));
        this.IPHostOrigem = InetAddress.getByName(informacao.substring(informacao.indexOf("HOST_REQ:[/") + 11, informacao.indexOf("]", informacao.indexOf("HOST_REQ:[") + 1)));
        this.portaHostOrigem = Integer.parseInt(informacao.substring(informacao.indexOf("PORTA_REQ:[") + 11, informacao.indexOf("]", informacao.indexOf("PORTA_REQ:[") + 1)));
        this.TTLReq = Integer.parseInt(informacao.substring(informacao.indexOf("TTL:[") + 5, informacao.indexOf("]", informacao.indexOf("TTL:[") + 1)));
    }


    public InetAddress getIPHostOrigem() {
        return this.IPHostOrigem;
    }

    public int getPortaHostOrigem() {
        return this.portaHostOrigem;
    }

    public String getNomeArquivoProcurado() {
        return this.nomeArquivoProcurado;
    }

    public UUID getUUIDRequisicao(){
        return this.UUIDRequisicao;
    }

    public String getTipo_mensagem() {
        return this.tipo_mensagem;
    }

    public int getTTLReq() {
        return this.TTLReq;
    }

    public void reenviarMensagem() {
        this.TTLReq = this.TTLReq - 1;
    }

    @Override
    public String toString() {
        return ("TIPO:[" + this.tipo_mensagem + "]-" +
                "HOST_REQ:[" + this.IPHostOrigem + "]-" +
                "PORTA_REQ:[" + this.portaHostOrigem + "]-" +
                "UUID_REQ:[" + this.UUIDRequisicao + "]-" +
                "NOME_ARQ:[" + this.nomeArquivoProcurado + "]-" +
                "TTL:[" + this.TTLReq + "]"
        );
    }

    public void setTipo_mensagem(String tipo_mensagem) {
        this.tipo_mensagem = tipo_mensagem;
    }
}
