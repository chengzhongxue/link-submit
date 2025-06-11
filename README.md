# link-submit
- 支持自助提交和修改友链


## 📃文档
https://docs.kunkunyu.com/docs/link-submit


## 交流群
* 添加企业微信 （备注进群）
  <img width="360" src="https://api.minio.yyds.pink/kunkunyu/files/2025/02/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20250212142105-pbceif.jpg" />

* QQ群
  <img width="360" src="https://api.minio.yyds.pink/kunkunyu/files/2025/05/qq-708998089-iqowsh.webp" />

## 主题适配

- 插件适配了友链页面提交按钮，如果不满意可以自己根据api来适配

### 调用搜索弹框

此插件是一个通用的提交友链框插件，主题需要做的只是通过 JS API 唤起提交友链框即可，以下是代码示例：

```html
<div th:if="${pluginFinder.available('link-submit')}">
    <a href="javascript:LinkSubmitWidget.open()" title="提交友链">
        提交友链
    </a>
</div>
```

其中，`pluginFinder.available('link-submit')` 的作用是判断使用者是否安装和启用了此插件，如果没有安装或者没有启用，那么就不会显示提交友链入口。


### 自定义样式

虽然目前不能直接为提交友链组件编写额外的样式，但可以通过一系列的 CSS 变量来自定义部分样式，开发者可以根据需求自行在主题中添加这些 CSS 变量，让提交友链组件和主题更好地融合。

目前已提供的 CSS 变量：

| 变量名                                              | 描述              |
|-----------------------------------------------------|-----------------|
| `--link-submit-widget-base-font-size`               | 基础字体大小       |
| `--link-submit-widget-base-font-family`             | 字体              |
| `--link-submit-widget-base-rounded`                 | 边框圆角           |
| `--link-submit-widget-base-bg-color`                | 基础背景色         |
| `--link-submit-widget-modal-layer-color`            | 模态框遮挡层背景色 |
| `--link-submit-widget-form-bg-color`                | 表单背景色         |
| `--link-submit-widget-form-border-color`            | 表单边框色         |
| `--link-submit-widget-form-text-color`              | 表单文本色         |
| `--link-submit-widget-form-label-color`             | 表单标签色         |
| `--link-submit-widget-form-placeholder-color`       | 表单占位符色       |
| `--link-submit-widget-form-button-bg-color`         | 按钮背景色         |
| `--link-submit-widget-form-button-text-color`       | 按钮文本色         |
| `--link-submit-widget-form-button-hover-bg-color`   | 按钮悬停背景色     |

