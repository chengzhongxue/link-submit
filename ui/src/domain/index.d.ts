export interface Metadata {
  'annotations'?: { [key: string]: string; };
  'creationTimestamp'?: string | null;
  'deletionTimestamp'?: string | null;
  'finalizers'?: Array<string> | null;
  'generateName'?: string;
  'labels'?: { [key: string]: string; };
  'name': string;
  'version'?: number | null;
}

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


