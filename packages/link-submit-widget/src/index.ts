import { LinkSubmitModal } from './link-submit-modal';

export { LinkSubmitModal };

const linkSubmitModalElement = document.createElement(
        'link-submit-modal'
) as LinkSubmitModal;

document.body.append(linkSubmitModalElement);

export function open() {
    linkSubmitModalElement.open = true;
}
