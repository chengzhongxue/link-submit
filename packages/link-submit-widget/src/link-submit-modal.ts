import resetStyles from '@unocss/reset/tailwind.css?inline';
import { styleMap } from 'lit/directives/style-map.js';
import { LitElement, PropertyValues, css, html, unsafeCSS } from 'lit';
import {property, state} from 'lit/decorators.js';
import { OverlayScrollbars } from 'overlayscrollbars';
import overlayscrollbarsStyles from 'overlayscrollbars/styles/overlayscrollbars.css?inline';
import baseStyles from './styles/base';

interface LinkGroup {
  displayName: string;
  groupName: string;
  priority: number;
}

interface ErrorResponse {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  requestId: string;
  timestamp: string;
}

interface SuccessResponse {
  spec: {
    status: string;
  };
}

export class LinkSubmitModal extends LitElement {
  @property({
    type: Boolean,
    reflect: true,
  })
  open = false;

  @state()
  private groups: LinkGroup[] = [];

  @state()
  private loading = true;

  @state()
  private submitting = false;

  @state()
  private toastMessage = '';

  @state()
  private toastType: 'success' | 'error' = 'success';

  @state()
  private selectedType = '';

  constructor() {
    super();
    this.fetchGroups();

    setTimeout(() => {
      const modalContent = this.shadowRoot?.querySelector(
        '.modal__content'
      ) as HTMLElement;
      if (modalContent) {
        OverlayScrollbars(modalContent, {
          scrollbars: {
            autoHide: 'scroll',
            autoHideDelay: 600,
          },
        });
      }
    }, 0);
  }

  override willUpdate(changedProperties: PropertyValues) {
    if (!changedProperties.has('open')) {
      return;
    }

    if (this.open) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.removeProperty('overflow');
    }
  }

  private showToast(message: string, type: 'success' | 'error' = 'success') {
    this.toastMessage = message;
    this.toastType = type;
    setTimeout(() => {
      this.toastMessage = '';
    }, 3000);
  }

  private async fetchGroups() {
    try {
      const response = await fetch('/apis/anonymous.link.submit.kunkunyu.com/v1alpha1/linkgroups');
      if (!response.ok) {
        throw new Error('Failed to fetch groups');
      }
      this.groups = await response.json();
    } catch (error) {
      console.error('Error fetching groups:', error);
    } finally {
      this.loading = false;
    }
  }

  private handleTypeChange(e: Event) {
    const select = e.target as HTMLSelectElement;
    this.selectedType = select.value;
  }

  private handleClose() {
    this.open = false;
  }

  private async handleSubmit(e: Event) {
    e.preventDefault();
    this.submitting = true;

    const form = e.target as HTMLFormElement;
    const formData = new FormData(form);

    const submitData = {
      url: formData.get('url'),
      displayName: formData.get('displayName'),
      logo: formData.get('logo'),
      description: formData.get('description'),
      oldUrl: formData.get('oldUrl'),
      email: formData.get('email'),
      groupName: formData.get('groupName'),
      rssUrl: formData.get('rssUrl'),
      type: formData.get('type'),
    };

    try {
      const response = await fetch('/apis/anonymous.link.submit.kunkunyu.com/v1alpha1/linksubmits/-/submit', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(submitData),
      });

      const result = await response.json();

      if (!response.ok) {
        const errorResponse = result as ErrorResponse;
        this.showToast(errorResponse.detail, 'error');
        return;
      }

      const successResponse = result as SuccessResponse;
      if (successResponse.spec.status === 'review') {
        this.showToast('提交成功，链接已审核');
      } else {
        this.showToast('提交成功，请等待审核');
      }
      
      // 延迟关闭表单，让用户看到提示
      setTimeout(() => {
        this.handleClose();
      }, 1000);
    } catch (error) {
      this.showToast(error instanceof Error ? error.message : '提交失败，请稍后重试', 'error');
    } finally {
      this.submitting = false;
    }
  }

  private linkSubmitForm() {
    return html`
      <div class="p-6 z-1 bg-base sticky top-0 border-form-border">
        <div class="flex flex-row-reverse items-center justify-between">
          <button 
            type="button" 
            tabindex="0" 
            class="text-xl text-form-label hover:text-form-text transition-colors"
            @click=${this.handleClose}
          >
            <svg 
              xmlns="http://www.w3.org/2000/svg" 
              class="w-6 h-6" 
              fill="none" 
              viewBox="0 0 24 24" 
              stroke="currentColor"
            >
              <path 
                stroke-linecap="round" 
                stroke-linejoin="round" 
                stroke-width="2" 
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
          <h2 class="text-xl font-semibold text-form-text">提交网站</h2>
        </div>
        <div class="mt-6">
          <form class="flex flex-col gap-6" @submit=${this.handleSubmit}>
            <div class="grid grid-cols-2 gap-4">
              <div class="flex flex-col gap-2">
                <label for="input-type" class="form-label">类型</label>
                <select 
                  id="input-type" 
                  name="type" 
                  required 
                  class="form-input"
                  @change=${this.handleTypeChange}
                >
                  <option value="add">添加</option>
                  <option value="update">修改</option>
                </select>
              </div>
              <div class="flex flex-col gap-2">
                <label for="input-group-name" class="form-label">网站分组</label>
                <select 
                  id="input-group-name" 
                  name="groupName" 
                  required 
                  class="form-input"
                  ?disabled=${this.loading}
                >
                  ${this.groups.map(group => html`
                    <option value="${group.groupName}">${group.displayName}</option>
                  `)}
                </select>
                ${this.loading ? html`
                  <div class="text-sm text-form-placeholder">加载中...</div>
                ` : ''}
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="flex flex-col gap-2">
                <label for="input-url" class="form-label">网址</label>
                <input type="url" name="url" id="input-url" placeholder="https://" required class="form-input">
              </div>
              <div class="flex flex-col gap-2">
                <label for="input-name" class="form-label">网站标题</label>
                <input type="text" name="displayName" id="input-name" required class="form-input">
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="flex flex-col gap-2">
                <label for="input-logo" class="form-label">logo</label>
                <input type="url" name="logo" id="input-logo" required class="form-input">
              </div>
              <div class="flex flex-col gap-2">
                <label for="input-url-rss" class="form-label">RSS地址</label>
                <input type="url" name="rssUrl" id="input-url-rss" class="form-input">
              </div>
            </div>

            <div class="flex flex-col gap-2">
              <label for="textarea-description" class="form-label">网站描述</label>
              <textarea id="textarea-description" name="description" rows="2" class="form-input"></textarea>
            </div>

            ${this.selectedType === 'update' ? html`
              <div class="flex flex-col gap-2">
                <label for="textarea-old-url" class="form-label">旧的网址</label>
                <input type="url" name="oldUrl" id="textarea-old-url" required class="form-input"></input>
              </div>
            ` : ''}

            <div>
              <label for="input-email" class="form-label">邮箱</label>
              <div class="flex items-end gap-4">
                <input
                  type="email"
                  name="email"
                  id="input-email"
                  class="form-input"
                  style="width: 50%;"
                />
                <button
                  type="submit"
                  class="form-button whitespace-nowrap ml-auto"
                  ?disabled=${this.submitting}
                >
                  ${this.submitting ? '提交中...' : '提交'}
                </button>
              </div>
              <div class="text-sm text-form-placeholder mt-1">用于接收审核结果通知</div>
            </div>
          </form>
        </div>
      </div>

      ${this.toastMessage ? html`
        <div class="fixed top-4 right-4 z-50 animate-fade-in">
          <div class="rounded-lg shadow-lg px-6 py-4 ${this.toastType === 'success' ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}">
            <div class="${this.toastType === 'success' ? 'text-green-800' : 'text-red-800'} font-medium">
              ${this.toastType === 'success' ? '成功' : '错误'}
            </div>
            <div class="${this.toastType === 'success' ? 'text-green-600' : 'text-red-600'} text-sm mt-1">
              ${this.toastMessage}
            </div>
          </div>
        </div>
      ` : ''}
    `;
  }

  override render() {
    return html`<div
      class="modal__wrapper"
      style="${styleMap({ display: this.open ? 'flex' : 'none' })}"
    >
      <div class="modal__layer" @click="${this.handleClose}"></div>
      <div
        data-overlayscrollbars-initialize
        class="modal__content shadow-xl bg-modal"
      >
        ${this.open ? this.linkSubmitForm() : ''}
      </div>
    </div>`;
  }

  static override styles = [
    unsafeCSS(resetStyles),
    unsafeCSS(overlayscrollbarsStyles),
    baseStyles,
    css`
      :host {
        // @deprecated --link-submit-widget-color-modal-layer and
        // --link-submit-widget-base-border-radius will be removed in future
        --base-rounded: var(
          --link-submit-widget-base-rounded,
          var(--link-submit-widget-base-border-radius, 0.4em)
        );
        --modal-layer-color: var(--link-submit-widget-modal-layer-color);
      }

      .modal__wrapper {
        position: fixed;
        left: 0px;
        top: 0px;
        display: flex;
        height: 100%;
        width: 100%;
        flex-direction: row;
        align-items: flex-start;
        justify-content: center;
        padding-top: 3em;
        padding-bottom: 3em;
        z-index: 999;
      }

      .modal__layer {
        background-color: var(--modal-layer-color, rgb(107 114 128 / 0.75));
        position: absolute;
        top: 0px;
        left: 0px;
        height: 100%;
        width: 100%;
        flex: none;
        animation: fadeIn 0.15s both;
        backdrop-filter: blur(4px);
      }

      .modal__content {
        border-radius: var(--base-rounded, 0.4em);
        position: relative;
        display: flex;
        flex-direction: column;
        align-items: stretch;
        width: calc(100vw - 20px);
        max-height: calc(100vh - 5em);
        max-width: 580px;
        overflow: auto;
        animation: fadeInUp 0.3s both;
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
        }

        to {
          opacity: 1;
        }
      }

      @keyframes fadeInUp {
        from {
          opacity: 0;
          transform: translate3d(0, 10%, 0);
        }

        to {
          opacity: 1;
          transform: translate3d(0, 0, 0);
        }
      }

      @unocss-placeholder;
    `,
  ];
}

customElements.get('link-submit-modal') || customElements.define('link-submit-modal', LinkSubmitModal);

declare global {
  interface HTMLElementTagNameMap {
    'link-submit-modal': LinkSubmitModal;
  }
}