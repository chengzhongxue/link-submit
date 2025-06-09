import {
  LinkSubmitSpecStatusEnum, LinkSubmitSpecTypeEnum
} from "@/api/generated";

export const linkSubmitStatusOptions = [
  {
    label: '审核',
    value: LinkSubmitSpecStatusEnum.Review,
  },
  {
    label: '待审核',
    value: LinkSubmitSpecStatusEnum.Pending,
  },
  {
    label: '拒绝',
    value: LinkSubmitSpecStatusEnum.Refuse,
  },
];

export const linkSubmitTypeOptions = [
  {
    label: '添加',
    value: LinkSubmitSpecTypeEnum.Add,
  },
  {
    label: '修改',
    value: LinkSubmitSpecTypeEnum.Update,
  },
];

