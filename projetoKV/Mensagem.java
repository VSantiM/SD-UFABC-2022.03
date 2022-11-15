package projetoKV;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Mensagem {
    private String tipo_mensagem; // PUT, GET, PUT_OK, REPLICATION, REPLICATION_OK
    private String key; // Key utilizada
    private InetAddress IPHostOrigem; // IP do Host solicitante
    private int portaHostOrigem; // Porta do Host solicitante
    private String value; // Value utilizado
    private long timestamp; // timestamp associado a key
    private InetAddress IPHostResposta;
    private int portaHostResposta;

    // Construtor para PUT
    public Mensagem(String tipo_mensagem, InetAddress IPHostOrigem, int portaHostOrigem, InetAddress IPHostResposta, int portaHostResposta, String key, String value) {
        this.tipo_mensagem = tipo_mensagem;
        this.IPHostOrigem = IPHostOrigem;
        this.portaHostOrigem = portaHostOrigem;
        this.IPHostResposta = IPHostResposta;
        this.portaHostResposta = portaHostResposta;
        this.key = key;
        this.value = value;
    }

    // Construtor para GET
    public Mensagem(String tipo_mensagem, InetAddress IPHostOrigem, int portaHostOrigem, InetAddress IPHostResposta, int portaHostResposta, String key, long timestamp) {
        this.tipo_mensagem = tipo_mensagem;
        this.IPHostOrigem = IPHostOrigem;
        this.portaHostOrigem = portaHostOrigem;
        this.IPHostResposta = IPHostResposta;
        this.portaHostResposta = portaHostResposta;
        this.timestamp = timestamp;
        this.key = key;
    }

    public Mensagem(String informacao) throws UnknownHostException {
        //Parser, a classe toString de Mensagem gerá a String no formato de ATRIBUTO:[VALOR_ATRIBUTO]-
        this.tipo_mensagem = informacao.substring(informacao.indexOf("TIPO:[") + 6, informacao.indexOf("]", informacao.indexOf("TIPO:[") + 1));
        this.key = informacao.substring(informacao.indexOf("KEY:[") + 5, informacao.indexOf("]", informacao.indexOf("KEY:[") + 1));
        this.value = informacao.substring(informacao.indexOf("VALUE:[") + 7, informacao.indexOf("]", informacao.indexOf("VALUE:[") + 1));
        this.IPHostOrigem = InetAddress.getByName(informacao.substring(informacao.indexOf("HOST_REQ:[/") + 11, informacao.indexOf("]", informacao.indexOf("HOST_REQ:[") + 1)));
        this.portaHostOrigem = Integer.parseInt(informacao.substring(informacao.indexOf("PORTA_REQ:[") + 11, informacao.indexOf("]", informacao.indexOf("PORTA_REQ:[") + 1)));
        this.IPHostResposta = InetAddress.getByName(informacao.substring(informacao.indexOf("HOST_RESPOSTA:[/") + 16, informacao.indexOf("]", informacao.indexOf("HOST_RESPOSTA:[") + 1)));
        this.portaHostResposta = Integer.parseInt(informacao.substring(informacao.indexOf("PORTA_RESPOSTA:[") + 16, informacao.indexOf("]", informacao.indexOf("PORTA_RESPOSTA:[") + 1)));
        this.timestamp = Long.parseLong(informacao.substring(informacao.indexOf("TS:[") + 4, informacao.indexOf("]", informacao.indexOf("TS:[") + 1)));
    }

    // GETTERS
    public String getTipo_mensagem() {
        return this.tipo_mensagem;
    }

    public String getKey() { return this.key; }

    public InetAddress getIPHostOrigem() { return this.IPHostOrigem; }

    public int getPortaHostOrigem() { return this.portaHostOrigem; }

    public String getValue() { return this.value; }

    public long getTimestamp() {
        return this.timestamp;
    }

    public InetAddress getIPHostResposta() { return this.IPHostResposta; }

    public int getPortaHostResposta() { return this.portaHostResposta; }


    @Override
    // a classe toString de Mensagem gerá a String no formato de ATRIBUTO:[VALOR_ATRIBUTO]-
    public String toString() {
        return ("TIPO:[" + this.tipo_mensagem + "]-" +
                "KEY:[" + this.key + "]-" +
                "VALUE:[" + this.value + "]-" +
                "HOST_REQ:[" + this.IPHostOrigem + "]-" +
                "PORTA_REQ:[" + this.portaHostOrigem + "]-" +
                "HOST_RESPOSTA:[" + this.IPHostResposta + "]-" +
                "PORTA_RESPOSTA:[" + this.portaHostResposta + "]-" +
                "TS:[" + this.timestamp + "]"
                + '\n'
        );
    }

    public void setTipo_mensagem(String tipo_mensagem) {
        this.tipo_mensagem = tipo_mensagem;
    }

    public void setTimestamp(long new_ts) { this.timestamp = new_ts; }

    public void setValue(String new_value) { this.value = new_value; }

    public void setIPHostResposta(InetAddress ip) { this.IPHostResposta = ip; }

    public void setPortaHostOrigem(int port) { this.portaHostResposta = port; }
}
