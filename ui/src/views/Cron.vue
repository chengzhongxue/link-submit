<script lang="ts" setup>
import {Toast, VButton} from "@halo-dev/components";
import {ref, nextTick} from "vue";
import type { CronLinkSubmit } from "@/api/generated";
import {linkSubmitCoreApiClient} from "@/api";
import {axiosInstance} from "@halo-dev/api-client";
import type {LinkGroupList} from "@/domain";
import {useMutation, useQuery} from "@tanstack/vue-query";

const Se = "cron-link-submit-default"

const formState = ref<CronLinkSubmit>({
  metadata: {
    name: Se,
    creationTimestamp: ""
  },
  spec: {
    cron: "@daily",
    suspend: false,
    cleanConfig: {
      type: "delete",
      withoutCheckGroupNames: [],
      moveGroupName: ""
    },
  },
  kind: "CronLinkSubmit",
  apiVersion: "link.submit.kunkunyu.com/v1alpha1",
});

const {isLoading: cronIsLoading, isFetching: cronIsFetching} = useQuery({
  queryKey: ["cron-douban"],
  queryFn: async () => {
    const {data} = await linkSubmitCoreApiClient.cronLinkSubmit.getCronLinkSubmit({
      name: Se
    },{
      mute: true
    });
    return data;
  },
  onSuccess(data) {
    formState.value =  data
  },
  retry: false
})

const { mutate:save, isLoading:saveIsLoading } = useMutation({
  mutationKey: ["cron-douban-save"],
  mutationFn: async () => {
    if (formState.value.metadata.creationTimestamp) {
      const { data: data } = await linkSubmitCoreApiClient.cronLinkSubmit.getCronLinkSubmit({
        name: Se
      });
      formState.value = {
        ...formState.value,
        status: data.status,
        metadata: data.metadata
      };
      return await linkSubmitCoreApiClient.cronLinkSubmit.updateCronLinkSubmit({
        name: Se,
        cronLinkSubmit: formState.value
      });
    }else {
      return await linkSubmitCoreApiClient.cronLinkSubmit.createCronLinkSubmit({
        cronLinkSubmit: formState.value
      });
    }
  },
  onSuccess(data) {
    formState.value = data.data
    Toast.success("保存成功");
  }
});


const handleSelectGroupRemote = {
  search: async ({ keyword, page, size }: { keyword: string; page: number; size: number }) => {
    const { data } = await axiosInstance.get<LinkGroupList>(
      `/apis/core.halo.run/v1alpha1/linkgroups`,{
        params: {
          page: page,
          size: size,
          keyword: keyword,
        },
      }
    );
    return {
      options: data.items.map((item) => ({
        label: item.spec?.displayName,
        value: item.metadata.name,
      })),
      total: data.total,
      page: data.page,
      size: data.size,
    };
  },
  findOptionsByValues: () => {
    return [];
  },
};

const cronOptions = [{
  label: "每月（每月 1 号 0 点）",
  value: "@monthly"
}, {
  label: "每周（每周第一天 的 0 点）",
  value: "@weekly"
}, {
  label: "每天（每天的 0 点）",
  value: "@daily"
}, {
  label: "每小时",
  value: "@hourly"
}]

</script>

<template>
  <Transition mode="out-in" name="fade">
    <div class=":uno: bg-white p-4">
      <div>
        <FormKit
          id="cron-setting"
          name="cron-setting"
          :preserve="true"
          type="form"
          :disabled="cronIsFetching"
          @submit="save"
        >
          <FormKit
            v-model="formState.spec.suspend"
            label="是否启用"
            type="checkbox"
            name="suspend"
          ></FormKit>
          <FormKit
            v-model="formState.spec.cron"
            label="定时表达式"
            type="select"
            name="cron"
            allow-create
            searchable
            help="定时表达式规则请参考：https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-cron-expression"
            validation="required"
            :options="cronOptions"
          ></FormKit>
          <FormKit type="group" label="定时清理无效友链设置" v-model="formState.spec.cleanConfig">
            <FormKit
              :options="[
                { label: '删除', value: 'delete'},
                { label: '移动（需设置无效友链分组）', value: 'move'},
              ]"
              name="type"
              type="radio"
              help="选择定时清理无效友链，执行的操作"
              validation="required"
            ></FormKit>
            <FormKit
              type="select"
              label="友链状态免校验分组"
              name="withoutCheckGroupNames"
              help="不需要校验的分组"
              searchable
              remote
              multiple
              :remote-option="handleSelectGroupRemote"
            />
            <FormKit
              v-if="formState.spec.cleanConfig?.type == 'move'"
              type="select"
              label="无效友链分组"
              name="moveGroupName"
              help="检测无效的友链移动至的分组"
              searchable
              remote
              :remote-option="handleSelectGroupRemote"
              validation="required"
            />
          </FormKit>
        </FormKit>
      </div>
      <div v-permission="['plugin:link:submit:manage']" class=":uno: pt-5">
        <div class=":uno: flex justify-start">
          <VButton
            :loading="saveIsLoading"
            :disabled="cronIsLoading"
            type="secondary"
            @click="$formkit.submit('cron-setting')"
          >
            保存
          </VButton>
        </div>
      </div>
    </div>
  </Transition>
</template>
