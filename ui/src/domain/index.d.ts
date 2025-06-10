import type {Metadata} from "@/api/generated";

export interface LinkGroup {
  'apiVersion': string;
  'kind': string;
  'metadata': Metadata;
  'spec'?: LinkGroupSpec;
}

export interface LinkGroupSpec {
  'displayName': string;
  'links'?: Array<string>;
  'priority'?: number;
}

export interface LinkGroupList {
  'first': boolean;
  'hasNext': boolean;
  'hasPrevious': boolean;
  'items': Array<LinkGroup>;
  'last': boolean;
  'page': number;
  'size': number;
  'total': number;
  'totalPages': number;
}


export interface Link {
  'apiVersion': string;
  'kind': string;
  'metadata': Metadata;
  'spec'?: LinkSpec;
}

export interface LinkSpec {
  'url': string;
  'displayName': string;
  'logo'?: string;
  'description'?: string;
  'priority'?: number;
  'groupName'?: string;
}

export interface LinkList {
  'first': boolean;
  'hasNext': boolean;
  'hasPrevious': boolean;
  'items': Array<Link>;
  'last': boolean;
  'page': number;
  'size': number;
  'total': number;
  'totalPages': number;
}

