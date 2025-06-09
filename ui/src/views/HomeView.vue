<script setup lang="ts">
import {markRaw, shallowRef} from "vue";
import Cron from "@/views/Cron.vue";
import { useRouteQuery } from "@vueuse/router";
import LinkVariantPlus from "~icons/mdi/link-variant-plus";

import {
  VCard,
  VPageHeader,
  VTabbar,
} from "@halo-dev/components";
import SubmitList from "@/views/SubmitList.vue";

const tabs = shallowRef([
  {
    id: "submitList",
    label: "提交记录",
    component: markRaw(SubmitList),
  },
  {
    id: "cron",
    label: "定时任务",
    component: markRaw(Cron),
  }

]);

const activeIndex = useRouteQuery<string>("tab", tabs.value[0].id);

</script>

<template>

  <VPageHeader title="友链自助提交管理">
    <template #icon>
      <LinkVariantPlus class="mr-2 self-center"/>
    </template>
  </VPageHeader>

  <div class="m-0 md:m-4">
    <VCard :body-class="['!p-0']">
      <template #header>
        <VTabbar
          v-model:active-id="activeIndex"
          :items="tabs.map((item) => ({ id: item.id, label: item.label }))"
          class="w-full !rounded-none"
          type="outline"
        ></VTabbar>
      </template>
      <div class="bg-white">
        <SubmitList ref="submitList" v-if="activeIndex=='submitList'"/>
        <Cron ref="cron" v-if="activeIndex=='cron'"/>
      </div>
    </VCard>
  </div>


</template>
