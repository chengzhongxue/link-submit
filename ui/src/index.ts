import { definePlugin } from "@halo-dev/console-shared";
import HomeView from "./views/HomeView.vue";
import { markRaw } from "vue";
import LinkVariantPlus from "~icons/mdi/link-variant-plus";

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: "ToolsRoot",
      route: {
        path: "/linkSubmit",
        name: "友链自助提交插件",
        component: HomeView,
        meta: {
          title: "友链自助提交插件",
          permissions: ["plugin:link:submit:view"],
          searchable: true,
          menu: {
            name: "友链自助提交管理",
            group: "tool",
            icon: markRaw(LinkVariantPlus),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
});
