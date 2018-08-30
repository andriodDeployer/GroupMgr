package group.transport;/**
 * Created by DELL on 2018/8/30.
 */

/**
 * user is lwb
 **/


public class IMProtocol {

    public static final int HEADER_SIZE = 17;
    public static final short MAGIC = (short) 0xcdcd;

    /**MESSAGE TYPE **/
    public static final byte TEXT                       = 0x01;     // 文本
    public static final byte IMAGE                      = 0x03;     // 图片
    public static final byte RESPONSE                   = 0x02;     // Response
    public static final byte HEARTBEAT                  = 0x0f;     // Heartbeat

    private byte messageCode;
    private byte serializerCode;    // sign 高地址4位
    private byte status;            // 响应状态码
    private long id;                // request.invokeId, 用于映射 <id, request, response> 三元组
    private int bodySize;           // 消息体长度


    public static byte toSign(byte serializerCode, byte messageCode) {
        return (byte) ((serializerCode << 4) | (messageCode & 0x0f));
    }

    public void sign(byte sign) {
        // sign 低地址4位
        this.messageCode = (byte) (sign & 0x0f);
        // sign 高地址4位, 先转成无符号int再右移4位
        this.serializerCode = (byte) ((((int) sign) & 0xff) >> 4);
    }

    public byte messageCode() {
        return messageCode;
    }

    public byte serializerCode() {
        return serializerCode;
    }

    public byte status() {
        return status;
    }

    public void status(byte status) {
        this.status = status;
    }

    public long id() {
        return id;
    }

    public void id(long id) {
        this.id = id;
    }

    public int bodySize() {
        return bodySize;
    }

    public void bodySize(int bodyLength) {
        this.bodySize = bodyLength;
    }


    @Override
    public String toString() {
        return "JProtocolHeader{" +
                "messageCode=" + messageCode +
                ", serializerCode=" + serializerCode +
                ", status=" + status +
                ", id=" + id +
                ", bodySize=" + bodySize +
                '}';
    }






}
