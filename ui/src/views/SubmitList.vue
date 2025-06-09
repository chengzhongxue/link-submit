<script lang="ts" setup>
import { 
  VCard,
  IconRefreshLine,
  Dialog,
  VButton,
  VEmpty,
  VLoading,
  VPagination,
  Toast,
  VSpace,
  VAvatar} from "@halo-dev/components";
import {useQuery, useQueryClient} from "@tanstack/vue-query";
import {computed, ref, watch} from "vue";
import { formatDatetime } from "@/utils/date";
import {useRouteQuery} from "@vueuse/router";
import {linkSubmitApiClient, linkSubmitCoreApiClient} from "@/api";
import {type LinkSubmit} from "@/api/generated";
import {axiosInstance} from "@halo-dev/api-client";
import type {LinkGroup, LinkGroupList} from "@/domain";
import {linkSubmitStatusOptions, linkSubmitTypeOptions} from "@/constant";

const queryClient = useQueryClient();

const selectedLinkSubmits = ref<string[]>([]);
const checkedAll = ref(false);
const keyword = useRouteQuery<string>("keyword", "");
const selectedSort = useRouteQuery<string | undefined>("sort");
const selectedStatus = useRouteQuery<string | undefined>("status");
const selectedType = useRouteQuery<string | undefined>("type");

const page = ref(1);
const size = ref(20);
const total = ref(0);


watch(
  () => [
    selectedSort.value,
    keyword.value,
    selectedStatus.value,
    selectedType.value,
  ],
  () => {
    page.value = 1;
  }
);

function handleClearFilters() {
  selectedSort.value = undefined;
  selectedStatus.value = undefined;
  selectedType.value = undefined;
}

const hasFilters = computed(() => {
  return (
    selectedSort.value ||
    selectedStatus.value ||
    selectedType.value

  );
});

const {
  data: linkSubmits,
  isLoading,
  isFetching,
  refetch,
} = useQuery({
  queryKey: ["link-submits", page, size,selectedSort,selectedStatus,selectedType,keyword],
  queryFn: async () => {
    
    const { data } = await linkSubmitApiClient.linkSubmit.listLinkSubmits(
      {
        page: page.value,
        size: size.value,
        sort: [selectedSort.value].filter(Boolean) as string[],
        status: selectedStatus.value,
        type : selectedType.value,
        keyword: keyword.value
      }
    );
    total.value = data.total;
    return data.items;
  },
  refetchInterval: (data) =>  {
    const deleting = data?.filter(
      (linkSubmit) => !!linkSubmit.metadata.deletionTimestamp
    );
    return deleting?.length ? 1000 : false;
  },
});

const {
  data: groups
} = useQuery<LinkGroup[]>({
  queryKey: ["link-groups"],
  queryFn: async () => {
    const {data} = await axiosInstance.get<LinkGroupList>(
      `/apis/core.halo.run/v1alpha1/linkgroups`
    );

    return data.items
      .map((group) => {
        if (group.spec) {
          group.spec.priority = group.spec.priority || 0;
        }
        return group;
      })
      .sort((a, b) => {
        return (a.spec?.priority || 0) - (b.spec?.priority || 0);
      });
  },
  refetchOnWindowFocus: false,
  refetchInterval(data) {
    const hasDeletingData = data?.some((group) => {
      return !!group.metadata.deletionTimestamp;
    });
    return hasDeletingData ? 1000 : false;
  },
});

function getGroup(groupName: string) {
  const linkGroup = groups.value?.find((group) => group.metadata.name === groupName);
  if (linkGroup?.spec) {
    return linkGroup?.spec.displayName;
  }
  return '未分组'
  
}



const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  checkedAll.value = checked;
  if (checkedAll.value) {
    selectedLinkSubmits.value =
      linkSubmits.value?.map((linkSubmit) => {
        return linkSubmit.metadata.name;
      }) || [];
  } else {
    selectedLinkSubmits.value.length = 0;
  }
};

const handleDeleteInBatch = () => {
  Dialog.warning({
    title: "是否确认删除所选的链接？",
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        const promises = selectedLinkSubmits.value.map((linkSubmit) => {
          return linkSubmitCoreApiClient.linkSubmit.deleteLinkSubmit({
            name: linkSubmit
          });
        });
        if (promises) {
          await Promise.all(promises);
        }
        selectedLinkSubmits.value.length = 0;
        checkedAll.value = false;

        Toast.success("删除成功");
      } catch (e) {
        console.error(e);
      } finally {
        queryClient.invalidateQueries({ queryKey: ["link-submits"] });
      }
    },
  });
};

const handleDelete = (linkSubmit: LinkSubmit) => {
  Dialog.warning({
    title: "确定删除吗？",
    description: "此操作不可逆，确定吗？",
    confirmType: 'danger',
    confirmText: "确定",
    cancelText: "取消",
    async onConfirm() {
      try {
        await linkSubmitCoreApiClient.linkSubmit.deleteLinkSubmit(
          {
            name: linkSubmit.metadata.name
          }
        )
        Toast.success("删除成功");
      } catch (error) {
        console.error("删除失败，请稍后再试", error);
      } finally {
        await queryClient.invalidateQueries({ queryKey: ["link-submits"] });
      }
    },
  });
}

function statusText (status:string) {
  const item = linkSubmitStatusOptions.find(option => option.value === status);

  return item ? item.label : "未知";
}

function typeText (type:string) {
  const item = linkSubmitTypeOptions.find(option => option.value === type);

  return item ? item.label : "未知";
}

</script>

<template>
  <VCard :body-class="['!p-0']">
    <template #header>
      <div class="block w-full bg-gray-50 px-4 py-3">
        <div class="relative flex flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center" >
          <div class="hidden items-center sm:flex" v-permission="['plugin:link:submit:manage']">
            <input
              v-model="checkedAll"
              type="checkbox"
              @change="handleCheckAllChange"
            />
          </div>
          <div class="flex w-full flex-1 items-center sm:w-auto" >
            <VSpace v-if="selectedLinkSubmits.length" v-permission="['plugin:link:submit:manage']">
              <VButton type="danger" @click="handleDeleteInBatch">
                删除
              </VButton>
            </VSpace>
            <SearchInput
              v-else
              v-model="keyword"
            />
          </div>
          <VSpace spacing="lg" class="flex-wrap">
            <FilterCleanButton
              v-if="hasFilters"
              @click="handleClearFilters"
            />
            <FilterDropdown
              v-model="selectedStatus"
              label="状态"
              :items="[
                {
                  label: '全部',
                  value: undefined,
                },
                ...linkSubmitStatusOptions
              ]"
            />
            <FilterDropdown
              v-model="selectedType"
              label="类型"
              :items="[
                  {
                    label: '全部',
                    value: undefined,
                  },
                  ...linkSubmitTypeOptions
              ]"
            />
            <div class="flex flex-row gap-2">
              <div
                class="group cursor-pointer rounded p-1 hover:bg-gray-200"
                @click="refetch()"
              >
                <IconRefreshLine
                  v-tooltip="'刷新'"
                  :class="{ 'animate-spin text-gray-900': isFetching }"
                  class="h-4 w-4 text-gray-600 group-hover:text-gray-900"
                />
              </div>
            </div>
          </VSpace>
        </div>
      </div>
    </template>
    <VLoading v-if="isLoading" />

    <Transition v-else-if="!linkSubmits?.length" appear name="fade">
      <VEmpty
        message="请尝试刷新"
        title="当前没有待审核的链接"
      >
        <template #actions>
          <VSpace>
            <VButton @click="refetch()"> 刷新 </VButton>
          </VSpace>
        </template>
      </VEmpty>
    </Transition>

    <Transition v-else appear name="fade">
      <div class="w-full relative overflow-x-auto">
        <table class="w-full text-sm text-left text-gray-500">
          <thead class="text-xs text-gray-700 uppercase bg-gray-50">
             <tr>
               <th v-permission="['plugin:link:submit:manage']" 
                   scope="col" class="px-4 py-3">
                 <div class="w-max flex items-center"> </div>
               </th>
               <th scope="col" class="px-4 py-3">
                 <div class="w-max flex items-center">网站LOGO </div>
               </th>
               <th scope="col" class="px-4 py-3">
                 <div class="w-max flex items-center">网站名称 </div>
               </th>
               <th scope="col" class="px-4 py-3">
                 <div class="w-max flex items-center">网站地址 </div>
               </th>
               <th scope="col" class="px-4 py-3">
                 <div class="w-max flex items-center">网站描述</div>
               </th>
               <th scope="col" class="px-4 py-3">
                 <div class="w-max flex items-center">邮箱</div>
               </th>
               <th scope="col" class="px-4 py-3">
                 <div class="w-max flex items-center">RSS 地址</div>
               </th>
               <th scope="col" class="px-4 py-3">
                 <div class="w-max flex items-center">友链分组</div>
               </th>
               <th scope="col" class="px-4 py-3">
                 <div class="w-max flex items-center">状态</div>
               </th>
               <th scope="col" class="px-4 py-3">
                 <div class="w-max flex items-center">类型</div>
               </th>
               <th scope="col" class="px-4 py-3">
                 <div class="w-max flex items-center">申请时间</div>
               </th>
               <th scope="col" class="px-4 py-3" v-permission="['plugin:link:submit:manage']">
                 <div class="w-max flex items-center">操作</div>
               </th>
             </tr>
          </thead>
          <tbody>
             <tr v-for="linkSubmit in linkSubmits" class="border-b last:border-none hover:bg-gray-100">
               <td class="px-4 py-4" 
                   v-permission="['plugin:link:submit:manage']">
                 <input
                   v-model="selectedLinkSubmits"
                   :value="linkSubmit.metadata.name"
                   class="h-4 w-4 rounded border-gray-300 text-indigo-600"
                   name="post-checkbox"
                   type="checkbox"
                 />
               </td>
               <td class="px-4 py-4 table-td">
                 <VAvatar
                   circle
                   :src="linkSubmit?.spec.logo"
                   :alt="linkSubmit?.spec.displayName"
                   size="md"
                 ></VAvatar>
               </td>
               <td class="px-4 py-4 table-td">{{ linkSubmit?.spec.displayName }}</td>
               <td class="px-4 py-4 table-td"><a :href="linkSubmit?.spec.url" target="_blank">{{ linkSubmit?.spec.url }}</a></td>
               <td class="px-4 py-4 table-td">{{ linkSubmit?.spec.description }}</td>
               <td class="px-4 py-4 table-td">{{ linkSubmit?.spec.email }}</td>
               <td class="px-4 py-4 table-td">
                 <a :href="linkSubmit?.spec.rssUrl" target="_blank">{{ linkSubmit?.spec.rssUrl }}</a>
               </td>
               <td class="px-4 py-4 table-td">
                 <span>
                   {{ getGroup(linkSubmit?.spec.groupName || '') }}
                 </span>
               </td>
               <td class="px-4 py-4 table-td">{{ statusText(linkSubmit?.spec.status) }}</td>
               <td class="px-4 py-4 table-td">{{ typeText(linkSubmit?.spec.type) }}</td>
               <td class="px-4 py-4 table-td">{{ formatDatetime(linkSubmit?.metadata.creationTimestamp) }}</td>
               <td class="px-4 py-4 table-td" v-permission="['plugin:link:submit:manage']">
                 <button>审核</button>&nbsp;&nbsp;
                 <button>修改链接</button>&nbsp;&nbsp;
                 <button @click="handleDelete(linkSubmit)">删除</button>
               </td>
             </tr>
          </tbody>
        </table>
      </div>
    </Transition>

    <template #footer>
      <VPagination
        v-model:page="page"
        v-model:size="size"
        :total="total"
        :size-options="[20, 30, 50, 100]"
      />
    </template>
  </VCard>
  
</template>

<style scoped lang="scss">

.table-td {
  text-align: left !important;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ant-link {
  max-width: 250px;
  border-bottom: 0;
  text-align: left !important;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ant-td {
  max-width: 410px;
  border-bottom: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: 3;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  padding-right: 1rem;
  padding-bottom: 0rem;
  margin-bottom: 1.1em;
}


</style>
