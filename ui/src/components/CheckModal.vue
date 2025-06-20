<script lang="ts" setup>
import {Toast, VButton, VModal, VSpace} from "@halo-dev/components";
import {defineEmits, ref, watch, nextTick} from "vue";
import {type CheckLinkSubmitRequest, type LinkSubmit, LinkSubmitSpecTypeEnum} from "@/api/generated";
import {linkSubmitApiClient} from "@/api";
import {useQueryClient} from "@tanstack/vue-query";
import {axiosInstance} from "@halo-dev/api-client";
import type {LinkList} from "@/domain";

const props = withDefaults(
  defineProps<{
    linkSubmit?: LinkSubmit;
  }>(),
  {
    linkSubmit: undefined,
  }
);

const emit = defineEmits<{
  (event: "close"): void;
}>();

const saving = ref<boolean>(false);
const formState = ref<CheckLinkSubmitRequest>({
  checkStatus: true,
  reason: "",
})
const queryClient = useQueryClient();
const modal = ref<InstanceType<typeof VModal> | null>(null);


const handleCheck = async () => {
  try {
    saving.value = true;
    await linkSubmitApiClient.linkSubmit.check({
      name: props.linkSubmit.metadata.name,
      checkLinkSubmitRequest: formState.value,
    });
    modal.value?.close();
    Toast.success('保存成功');
  } catch (error) {
    console.error("Failed to check LinkSubmit", error);
  } finally {
    queryClient.invalidateQueries({
      queryKey: ["link-submits"]
    })
    saving.value = false;
  }
};

const handleSelectLinkRemote = {
  search: async ({ keyword, page, size }: { keyword: string; page: number; size: number }) => {
    const { data } = await axiosInstance.get<LinkList>(
      `/apis/api.plugin.halo.run/v1alpha1/plugins/PluginLinks/links`,{
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

</script>
<template>
  <VModal
    ref="modal"
    :title="linkSubmit.spec.type == LinkSubmitSpecTypeEnum.Add ? '友链提交审核' : '友链修改审核'"
    :width="650"
    @close="emit('close')"
  >
    <FormKit
        id="check-form"
        v-model="formState"
        name="check-form"
        type="form"
        :config="{ validationVisibility: 'submit' }"
        @submit="handleCheck"
    >
      <FormKit
        :options="[
            { label: '通过', value: true},
            { label: '不通过', value: false},
          ]"
        label="审核状态"
        name="checkStatus"
        type="select"
      ></FormKit>
      <FormKit
        v-if="linkSubmit.spec.type == LinkSubmitSpecTypeEnum.Update"
        type="select"
        label="链接"
        name="linkName"
        help="选择修改的友链"
        searchable
        remote
        :remote-option="handleSelectLinkRemote"
        validation="required"
      />
      <FormKit
        v-if="formState.checkStatus === false"
        type="textarea"
        name="reason"
        label="审核说明"
        placeholder="请输入审核说明"
        validation="required"
        rows="3"
      ></FormKit>
    </FormKit>

    <template #footer>
      <VSpace>
        <VButton
            :loading="saving"
            type="secondary"
            @click="$formkit.submit('check-form')"
        >
          提交
        </VButton>
        <VButton @click="modal?.close()">
          取消
        </VButton>
      </VSpace>
    </template>
  </VModal>
</template>
