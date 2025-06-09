<script lang="ts" setup>
import {Toast, VButton, VModal, VSpace} from "@halo-dev/components";
import {defineEmits, ref, watch, nextTick} from "vue";
import type {LinkSubmit} from "@/api/generated";
import {linkSubmitApiClient, linkSubmitCoreApiClient} from "@/api";
import { submitForm } from "@formkit/core";
import {useQueryClient} from "@tanstack/vue-query";

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
const formState = ref({
  checkStatus: true,
  reason: "",
})
const queryClient = useQueryClient();
const modal = ref<InstanceType<typeof VModal> | null>(null);


const handleCheck = async () => {
  try {
    saving.value = true;
    await linkSubmitApiClient.linkSubmit.createTag({
      tag: formState.value,
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

</script>
<template>
  <VModal
    ref="modal"
    title="审核操作"
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
