import { definePlugin } from "@halo-dev/ui-shared";
import { markRaw } from "vue";
import LinkVariantPlus from "~icons/mdi/link-variant-plus";
import "uno.css";

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: "ToolsRoot",
      route: {
        path: "/link-submit",
        name: "友链自助提交插件",
        component: () => import("@/views/HomeView.vue"),
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
