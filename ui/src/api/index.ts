import { axiosInstance } from "@halo-dev/api-client";
import {
  ConsoleApiLinkSubmitKunkunyuComV1alpha1ListLinkSubmitApi,
  LinkSubmitV1alpha1Api,
  CronLinkSubmitV1alpha1Api

} from "./generated";


const linkSubmitCoreApiClient = {
  linkSubmit: new LinkSubmitV1alpha1Api(undefined, "", axiosInstance),
  cronLinkSubmit: new CronLinkSubmitV1alpha1Api(undefined, "", axiosInstance),
};

const linkSubmitApiClient = {
  linkSubmit: new ConsoleApiLinkSubmitKunkunyuComV1alpha1ListLinkSubmitApi(undefined,"",axiosInstance),
};
export { linkSubmitCoreApiClient, linkSubmitApiClient };
