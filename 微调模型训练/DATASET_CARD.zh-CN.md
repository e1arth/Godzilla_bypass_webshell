# Dataset Card（数据集说明）

## 1. 数据集概览

- 名称：`nbuser32/PHP-Webshell-Dataset`
- 公开地址：https://huggingface.co/datasets/nbuser32/PHP-Webshell-Dataset
- 许可证：MIT
- 任务类型：PHP Webshell 二分类
- 标签定义：`0=benign`, `1=webshell`

## 2. 数据来源与构建方式

- 原始字段：`instruction`, `input`, `output`
- 标签提取方式：从 `output` 首行解析 `True/False`，映射到 `1/0`
- 有效样本条件：
  - `output` 可解析为 `True/False`
  - `input` 非空
- 切分策略：
  - 上游公开集只有 `train`
  - 本项目在本地按 `8/1/1` 做分层切分（`seed=42`）

## 3. 标注与语义说明

- `True`：判定为恶意/高风险 webshell 语义
- `False`：判定为良性
- 注意：该数据集的指令语义偏“高敏感”，可能导致模型更偏召回导向

## 4. 数据统计

- 总样本数：`30,270`
- 全量标签分布：
  - `webshell(1)=16,324`（53.928%）
  - `benign(0)=13,946`（46.072%）
- 本地切分规模（来自训练日志）：
  - `train=24,216`
  - `val=3,027`
  - `test=3,027`
- 说明：采用分层切分，子集标签比例与全量接近

## 5. 偏差与局限

- 指令语义可能偏“宁可错杀”的高敏感策略
- 与真实生产代码分布（框架代码、业务代码、依赖库）可能存在差距
- 对真实 benign 误报表现需额外评估

## 6. 安全与合规

- 仅用于授权安全研究与防御测试
- 不应用于未授权系统扫描或攻击
- 建议保留评估日志与人工复核记录

## 7. 复现信息

- 数据下载：
  - `datasets.load_dataset("nbuser32/PHP-Webshell-Dataset", split="train")`
- 标签解析：
  - 正则 `^\\s*(true|false)\\b`（忽略大小写）
- 固定随机种子：
  - `seed=42`
