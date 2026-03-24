<h1 align="center">VeilShell</h1>

### Godzilla_AES加密器+采用打断与动态回调伪装的WebShell以及另一种自定义Stream Wrapper去除eval的webshell|Qwen2-0.5B-Instruc-webshell微调小模型检测方法与对抗。
插件是基于哥斯拉底层反射的自定义AES通信加密器

bypass_webshell.py基于AES+gzdeflate+Data-Flow Break(把它还放着主要是因为不依赖 stream wrapper，兼容低版本PHP，效果也还行)

**bypass_webshell_vei.py**基于AES+自定义Stream Wrapper注册+include执行，全链路零eval，类多态触发（部分严格环境如禁用stream_wrapper_register时无法使用）。

```mermaid
graph TD
    A["Godzilla Client<br/>POST + Cookie"] -->|"HTTP Request"| B["Cookie Gate<br/>filter_input(INPUT_COOKIE)"]
    B -->|"cookie_key = AES密钥 (16字节)"| C["外层解密<br/>hex2bin → openssl_decrypt<br/>AES-128-ECB"]
    C -->|"gzinflate 解压"| D["$s = 明文 Stager 代码"]

    subgraph 外层执行 ["外层执行 (磁盘文件 · 零eval · 零$GLOBALS)"]
        D -->|"CacheStream::$d = '<?php ' . $s"| E["外层 Stream Wrapper<br/>静态属性 self::$d 传递"]
        E -->|"include('scheme://1')<br/>多态触发: __construct 或 __invoke"| F["stream_read()<br/>从 self::$d 读取代码"]
    end

    F -->|"Stager 开始执行"| G{首次请求?}

    G -->|"是 · 含 getBasicsInfo"| H["aesEnc(payload) → $_SESSION<br/>session_write_close()<br/>echo chr(32)"]
    H -->|"Payload 缓存到内存"| A

    G -->|"否 · Session 已有缓存"| I["aesDec($_SESSION) → $payload"]

    subgraph 内层执行 ["内层执行 (纯内存 · 递归Stream Wrapper · 零eval)"]
        I -->|"IS::$d = '<?php ' . $payload"| J["内层 Stream Wrapper<br/>随机协议名 a+md5(rand)"]
        J -->|"include('随机协议://1')"| K["stream_read()<br/>从 IS::$d 读取 Payload"]
        K -->|"定义 run() 函数"| L["stream_wrapper_unregister<br/>协议用完即销毁"]
    end

    L --> M["ob_start()<br/>$result = run($data)"]
    M --> N["响应格式化<br/>sid前16 + base64(aesEnc) + sid后16"]
    N -->|"HTTP Response"| A
```
-----     
```mermaid
sequenceDiagram
    participant K as PHP Kernel
    participant W as CacheStream (Wrapper)
    participant S as self::$d (静态属性)

    Note over K,S: 由 include('scheme://1') 触发

    K->>W: stream_open('scheme://1')
    W-->>K: return true

    K->>W: stream_stat()
    W-->>K: return [] (空stats)

    loop Until EOF
        K->>W: stream_read($length)
        W->>S: 读取 self::$d
        S-->>W: Raw PHP Code
        W-->>K: 返回代码片段
    end

    K->>W: stream_eof()
    W-->>K: return true

    Note over K: PHP 将返回的代码当作本地文件执行，全链路零 eval()
```


## 本项目生成的荷载在Qwen2-0.5B-Instruct模型中经过30k webshell数据集训练微调后的小模型分析，并未命中。同时在长亭、阿里等webshell检测中也绕过。

对于结果有疑虑可阅读：[Qwen2-0.5B-Instruc-webshell微调模型检测训练](./微调模型训练/README.md) 

注：该图展示的样本是二次过滤后的恶意样本，选了40+能过waf的phpwebshell进行测试。并不代表全量训练数据集，全量数据集采用了https://huggingface.co/datasets/nbuser32/PHP-Webshell-Dataset

<img width="1640" height="729" alt="b02a5230965decbd5961bc86453f3b47" src="https://github.com/user-attachments/assets/90c0cec6-ab67-411e-923d-fe0ee1ee34a7" />
<img width="1640" height="793" alt="image" src="https://github.com/user-attachments/assets/90fab391-3481-4850-a29b-f393825f52ac" />

> Test metrics: {'test_loss': 0.08689013123512268, 'test_accuracy': 0.973571192599934, 'test_f1': 0.9750623441396509, 'test_precision': 0.993015873015873, 'test_recall': 0.9577464788732394, 'test_runtime': 71.2095, 'test_samples_per_second': 42.508, 'test_steps_per_second': 2.668, 'epoch': 1.0}
---- 
---- 
### 长亭
<img width="800" height="398" alt="8e6f29ea85008ff0594cc713b558c421" src="https://github.com/user-attachments/assets/6cebe93a-166a-45f4-9ec3-f35738970f4c" />

### 阿里
<img width="800" height="398" alt="ba812ce86962430d9da1087bdf10cc62" src="https://github.com/user-attachments/assets/87e08138-b07a-41bd-985d-365c46e3cb7b" />


### virustotal
<img width="800" height="398" alt="image" src="https://github.com/user-attachments/assets/b2cfb8ea-aedd-4037-a515-092689208f80" />

正常连接及环境：
<img width="800" height="398" alt="image" src="https://github.com/user-attachments/assets/f84d4826-300f-4ec4-b158-8ac9976637f7" />

### post

<img width="800" height="398" alt="90958e5691087a8976449ebc70f860d0" src="https://github.com/user-attachments/assets/05b558d0-9a3a-400d-b5fc-fb579cc81604" />
