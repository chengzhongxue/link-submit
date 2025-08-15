/// <reference types="unplugin-icons/types/vue" />
/// <reference types="@rsbuild/core/types" />

declare module "axios" {
  export interface AxiosRequestConfig {
    mute?: boolean;
  }
}

declare module "vue" {
  interface ComponentCustomProperties {
    $formkit: any;
  }
}
