# 模型说明

## 1. 模型信息

- 模型名称：Webshell-Qwen2-0.5B-Detector（LoRA Adapter）
- 基座模型：`Qwen/Qwen2-0.5B-Instruct`
- 微调方式：LoRA
- 任务：PHP Webshell Detection（二分类）
- 数据集：`nbuser32/PHP-Webshell-Dataset`

## 2. 训练配置

- 硬件：Google Colab `Tesla T4`
- 学习率：`2e-4`
- Batch Size：`16`
- Gradient Accumulation：`1`
- 最大长度：`256`
- Epoch：`1`
- 优化器：`adamw_torch`
- 随机种子：`42`
- 训练步数：`1514`
- 训练耗时（日志）：约 `23:24`

## 3. 输入输出定义

- 输入：PHP 源码文本
- 模型原始输出：
  - `model_prob`：恶意概率
  - `prediction`：`benign` / `webshell`
- 融合输出（模型 + 静态规则）：
  - `rule_prob`
  - `fused_score`
  - `risk_level`
  - `action`

## 4. 测试集结果（模型 only）

> 结果来自 notebook 的 `trainer.evaluate(test_tok)` 输出

| 指标 | 值 |
|---|---:|
| test_loss | 0.086890 |
| test_accuracy | 0.973571 |
| test_precision | 0.993016 |
| test_recall | 0.957746 |
| test_f1 | 0.975062 |

## 5. 融合策略参数（当前实验）

- `W_MODEL = 0.65`
- `W_RULE = 0.35`
- `TH_REVIEW = 0.78`
- `TH_HIGH = 0.95`

策略：

- `fused_score >= TH_HIGH` -> `block`
- `TH_REVIEW <= fused_score < TH_HIGH` -> `manual_review`
- `< TH_REVIEW` -> `allow`

## 6. 局限

- 训练与 test 评估阶段已包含公开数据集中的 benign 与 webshell 样本
- 当前 `tools/php_fusion_scan.csv`（42 文件）用于融合策略演示，样本分布偏恶意
- 阈值尚未按真实生产误报率目标进行系统校准（这里特指引入waf，但样本本身是经过挑选通过waf或只是简单的，所以不再做校准）

## 7. 责任与合规

- 仅用于授权安全研究与防御场景
- 不用于未授权扫描、入侵或攻击行为
