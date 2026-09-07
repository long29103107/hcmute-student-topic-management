document.addEventListener('DOMContentLoaded', () => {
    const trimInputTypes = new Set(['text', 'email', 'search', 'tel', 'url']);
    document.querySelectorAll('form').forEach((form) => {
        form.addEventListener('submit', () => {
            form.querySelectorAll('input, textarea').forEach((field) => {
                const type = (field.type || '').toLowerCase();
                if (field instanceof HTMLTextAreaElement || trimInputTypes.has(type)) {
                    field.value = field.value.trim();
                }
            });
        });
    });

    const toastElements = [...document.querySelectorAll('[data-toast]')];
    const maxToasts = 3;
    toastElements.slice(0, Math.max(0, toastElements.length - maxToasts)).forEach((toast) => toast.remove());

    toastElements.slice(-maxToasts).forEach((toast) => {
        const dismissButton = toast.querySelector('[data-toast-dismiss]');
        const countdown = toast.querySelector('[data-toast-countdown]');
        const duration = Math.max(1, Number.parseInt(toast.dataset.toastDuration || '5', 10));
        let remaining = duration;
        let dismissTimer;
        let countdownTimer;

        const dismissToast = () => {
            window.clearTimeout(dismissTimer);
            window.clearInterval(countdownTimer);
            toast.classList.add('translate-x-4', 'opacity-0');
            window.setTimeout(() => toast.remove(), 200);
        };

        dismissButton?.addEventListener('click', dismissToast);
        countdownTimer = window.setInterval(() => {
            remaining -= 1;
            if (countdown) {
                countdown.textContent = String(Math.max(0, remaining));
            }
        }, 1000);
        dismissTimer = window.setTimeout(dismissToast, duration * 1000);
    });

    document.querySelectorAll('[data-user-status]').forEach((status) => {
        const active = status.textContent.trim() === 'Active';
        status.classList.add('inline-flex', 'items-center', 'rounded-full', 'px-2.5', 'py-1', 'text-xs', 'font-medium');
        status.classList.toggle('bg-green-100', active);
        status.classList.toggle('text-green-800', active);
        status.classList.toggle('bg-yellow-100', !active);
        status.classList.toggle('text-yellow-800', !active);
    });

    const sidebar = document.querySelector('[data-sidebar]');
    const appShell = document.querySelector('[data-app-shell]');
    const sidebarToggle = document.querySelector('[data-sidebar-toggle]');
    const sidebarToggleIcon = document.querySelector('[data-sidebar-toggle-icon]');
    const sidebarToggleLabel = document.querySelector('[data-sidebar-toggle-label]');
    const sidebarLabels = [...document.querySelectorAll('[data-sidebar-label]')];
    const sidebarLinks = [...document.querySelectorAll('[data-sidebar-link]')];
    const sidebarBrand = document.querySelector('[data-sidebar-brand]');
    const sidebarStorageKey = 'hcmute-topic-manager.sidebar-collapsed';

    const readSidebarPreference = () => {
        try {
            return window.localStorage.getItem(sidebarStorageKey) === 'true';
        } catch (error) {
            return false;
        }
    };

    const writeSidebarPreference = (collapsed) => {
        try {
            window.localStorage.setItem(sidebarStorageKey, String(collapsed));
        } catch (error) {
            // Ignore storage restrictions; the toggle still works for this page.
        }
    };

    const applySidebarState = (collapsed) => {
        if (!sidebar || !appShell) {
            return;
        }

        sidebar.classList.toggle('md:w-20', collapsed);
        sidebar.classList.toggle('md:w-64', !collapsed);
        appShell.classList.toggle('md:ml-20', collapsed);
        appShell.classList.toggle('md:ml-64', !collapsed);
        sidebarLabels.forEach((label) => label.classList.toggle('md:hidden', collapsed));
        sidebarLinks.forEach((link) => link.classList.toggle('md:justify-center', collapsed));
        if (sidebarBrand) {
            sidebarBrand.classList.toggle('md:justify-center', collapsed);
            sidebarBrand.classList.toggle('md:gap-0', collapsed);
        }
        if (sidebarToggle) {
            sidebarToggle.setAttribute('aria-expanded', String(!collapsed));
            sidebarToggle.setAttribute('aria-label', collapsed ? 'Expand sidebar' : 'Collapse sidebar');
            sidebarToggle.title = collapsed ? 'Expand sidebar' : 'Collapse sidebar';
        }
        if (sidebarToggleLabel) {
            sidebarToggleLabel.textContent = collapsed ? 'Expand sidebar' : 'Collapse sidebar';
        }
        if (sidebarToggleIcon) {
            sidebarToggleIcon.classList.toggle('rotate-180', collapsed);
        }
    };

    applySidebarState(readSidebarPreference());
    sidebarToggle?.addEventListener('click', () => {
        const collapsed = !sidebar.classList.contains('md:w-20');
        applySidebarState(collapsed);
        writeSidebarPreference(collapsed);
    });

    document.querySelectorAll('[data-action="reload"]').forEach((button) => {
        button.addEventListener('click', () => window.location.reload());
    });

    const roleSearch = document.querySelector('[data-role-search]');
    const roleItems = [...document.querySelectorAll('[data-role-item]')];
    const roleFilterEmpty = document.querySelector('[data-role-filter-empty]');

    if (roleSearch) {
        const filterRoles = () => {
            const query = roleSearch.value.trim().toLowerCase();
            let visibleCount = 0;

            roleItems.forEach((item) => {
                const matches = !query || (item.dataset.roleSearchValue || '').toLowerCase().includes(query);
                item.hidden = !matches;
                if (matches) {
                    visibleCount += 1;
                }
            });

            if (roleFilterEmpty) {
                roleFilterEmpty.hidden = roleItems.length === 0 || visibleCount > 0;
            }
        };

        roleSearch.addEventListener('input', filterRoles);
    }

    const permissionSearch = document.querySelector('[data-permission-search]');
    const permissionGroups = [...document.querySelectorAll('[data-permission-group]')];
    const permissionFilterEmpty = document.querySelector('[data-permission-filter-empty]');
    const permissionInputs = [...document.querySelectorAll('[data-permission-input]')];
    const selectedCount = document.querySelector('[data-selected-count]');

    const syncSelectedCount = () => {
        if (selectedCount) {
            selectedCount.textContent = permissionInputs.filter((input) => input.checked).length;
        }
    };

    const syncGroupToggle = (group) => {
        const inputs = [...group.querySelectorAll('[data-permission-input]')];
        const toggle = group.querySelector('[data-group-toggle]');
        if (!toggle || inputs.length === 0) {
            return;
        }
        toggle.checked = inputs.every((input) => input.checked);
        toggle.indeterminate = !toggle.checked && inputs.some((input) => input.checked);
    };

    const filterPermissions = () => {
        const query = permissionSearch ? permissionSearch.value.trim().toLowerCase() : '';
        let visibleCount = 0;

        permissionGroups.forEach((group) => {
            const items = [...group.querySelectorAll('[data-permission-item]')];
            let visibleInGroup = 0;

            items.forEach((item) => {
                const matches = !query || (item.dataset.permissionSearchValue || '').toLowerCase().includes(query);
                item.hidden = !matches;
                if (matches) {
                    visibleInGroup += 1;
                    visibleCount += 1;
                }
            });

            group.hidden = visibleInGroup === 0;
        });

        if (permissionFilterEmpty) {
            permissionFilterEmpty.hidden = !query || visibleCount > 0;
        }
    };

    permissionGroups.forEach((group) => {
        const trigger = group.querySelector('[data-group-trigger]');
        const body = group.querySelector('[data-group-body]');
        const chevron = group.querySelector('[data-group-chevron]');
        const toggle = group.querySelector('[data-group-toggle]');

        if (trigger && body) {
            const setGroupExpanded = (expanded) => {
                body.setAttribute('aria-hidden', String(!expanded));
                body.style.maxHeight = expanded ? `${body.scrollHeight}px` : '0px';
            };

            setGroupExpanded(true);
            trigger.addEventListener('click', () => {
                const isExpanded = trigger.getAttribute('aria-expanded') === 'true';
                trigger.setAttribute('aria-expanded', String(!isExpanded));
                setGroupExpanded(!isExpanded);
                chevron?.classList.toggle('rotate-180', isExpanded);
            });
        }

        if (toggle) {
            toggle.addEventListener('change', () => {
                group.querySelectorAll('[data-permission-input]').forEach((input) => {
                    input.checked = toggle.checked;
                });
                toggle.indeterminate = false;
                syncSelectedCount();
            });
        }

        group.querySelectorAll('[data-permission-input]').forEach((input) => {
            input.addEventListener('change', () => {
                syncGroupToggle(group);
                syncSelectedCount();
            });
        });

        syncGroupToggle(group);
    });

    if (permissionSearch) {
        permissionSearch.addEventListener('input', filterPermissions);
    }

    syncSelectedCount();

    const userSearch = document.querySelector('[data-user-search]');
    const userSearchForm = document.querySelector('[data-user-search-form]');
    if (userSearch && userSearchForm) {
        let searchTimer;
        const submitSearch = () => {
            window.clearTimeout(searchTimer);
            searchTimer = window.setTimeout(() => userSearchForm.requestSubmit(), 350);
        };
        userSearch.addEventListener('input', submitSearch);
        userSearchForm.addEventListener('submit', () => {
            const page = userSearchForm.querySelector('input[name="page"]');
            if (page) {
                page.value = '0';
            }
        });
    }

    document.querySelectorAll('[data-user-form]').forEach((form) => {
        const roleChoices = [...form.querySelectorAll('[data-user-role-choice]')];
        const accountType = (form.dataset.accountType || '').toUpperCase();
        const studentFields = form.querySelector('[data-student-fields]');
        const studentCode = form.querySelector('[data-student-code]');
        const loginIdentifier = form.querySelector('[data-user-login]');
        const email = form.querySelector('[data-user-email]');
        const emailLabel = form.querySelector('[data-user-email-label]');
        const emailHelp = form.querySelector('[data-user-email-help]');
        const emailTooltip = form.querySelector('[data-user-email-tooltip]');

        const syncUserType = () => {
            const isStudent = accountType === 'STUDENT' || roleChoices
                .filter((choice) => choice.checked)
                .some((choice) => (choice.dataset.roleCode || '').toUpperCase() === 'STUDENT');

            if (studentFields) {
                studentFields.hidden = !isStudent;
            }
            if (email) {
                email.readOnly = isStudent;
                email.required = !isStudent;
                if (isStudent && studentCode) {
                    const code = studentCode.value.trim();
                    email.value = code ? code + (email.dataset.studentEmailDomain || '') : '';
                }
            }
            if (loginIdentifier && isStudent && studentCode) {
                loginIdentifier.value = studentCode.value.trim();
            }
            if (emailLabel) {
                emailLabel.textContent = isStudent ? 'Email (generated)' : 'Email';
            }
            if (emailHelp) {
                const helpText = isStudent
                    ? 'Generated from the student code and cannot be edited.'
                    : 'Enter the lecturer email address.';
                if (emailTooltip) {
                    emailTooltip.textContent = helpText;
                }
            }
        };

        roleChoices.forEach((choice) => choice.addEventListener('change', syncUserType));
        studentCode?.addEventListener('input', syncUserType);
        syncUserType();
    });

    document.querySelectorAll('[data-password-form]').forEach((form) => {
        const password = form.querySelector('[data-password-input]');
        const confirmation = form.querySelector('[data-password-confirm]');
        const mismatch = form.querySelector('[data-password-mismatch]');

        if (!password || !confirmation) {
            return;
        }

        const syncPasswordConfirmation = () => {
            const isMismatch = confirmation.value.length > 0 && password.value !== confirmation.value;
            confirmation.setCustomValidity(isMismatch ? 'Passwords do not match.' : '');
            mismatch?.classList.toggle('hidden', !isMismatch);
        };

        password.addEventListener('input', syncPasswordConfirmation);
        confirmation.addEventListener('input', syncPasswordConfirmation);
        form.addEventListener('submit', syncPasswordConfirmation);
    });

    document.querySelectorAll('[data-confirm]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (!window.confirm(form.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    const reviewModal = document.querySelector('[data-review-confirm-modal]');
    const reviewForms = [...document.querySelectorAll('[data-review-form]')];
    if (reviewModal && reviewForms.length > 0) {
        const reviewModalTitle = reviewModal.querySelector('[data-review-modal-title]');
        const reviewModalDescription = reviewModal.querySelector('[data-review-modal-description]');
        const reviewModalTopicTitle = reviewModal.querySelector('[data-review-modal-topic-title]');
        const reviewModalConfirm = reviewModal.querySelector('[data-review-modal-confirm]');
        const reviewModalCancel = reviewModal.querySelector('[data-review-modal-cancel]');
        const reviewModalIcons = [...reviewModal.querySelectorAll('[data-review-modal-icon]')];
        let pendingReviewForm;
        let lastFocusedReviewButton;

        const closeReviewModal = () => {
            reviewModal.classList.add('hidden');
            reviewModal.setAttribute('aria-hidden', 'true');
            document.body.classList.remove('overflow-hidden');
            pendingReviewForm = undefined;
            lastFocusedReviewButton?.focus();
        };

        const openReviewModal = (form, button) => {
            const decision = button.dataset.reviewDecision === 'reject' ? 'reject' : 'approve';
            const topicTitle = button.dataset.reviewTopicTitle || 'this topic proposal';
            const approving = decision === 'approve';

            pendingReviewForm = form;
            lastFocusedReviewButton = button;
            if (reviewModalTitle) {
                reviewModalTitle.textContent = approving ? 'Approve topic proposal?' : 'Reject topic proposal?';
            }
            if (reviewModalDescription) {
                reviewModalDescription.textContent = approving
                    ? 'The proposal will move forward in the topic workflow.'
                    : 'The proposal will be returned to the proposer for revision.';
            }
            if (reviewModalTopicTitle) {
                reviewModalTopicTitle.textContent = topicTitle;
            }
            if (reviewModalConfirm) {
                reviewModalConfirm.textContent = approving ? 'Approve topic' : 'Reject topic';
                reviewModalConfirm.classList.remove(
                    'bg-emerald-600', 'hover:bg-emerald-700', 'focus:ring-emerald-200',
                    'bg-rose-600', 'hover:bg-rose-700', 'focus:ring-rose-200');
                reviewModalConfirm.classList.add(
                    approving ? 'bg-emerald-600' : 'bg-rose-600',
                    approving ? 'hover:bg-emerald-700' : 'hover:bg-rose-700',
                    approving ? 'focus:ring-emerald-200' : 'focus:ring-rose-200');
            }
            reviewModalIcons.forEach((icon) => {
                icon.classList.toggle('hidden', icon.dataset.reviewModalIcon !== decision);
                icon.classList.toggle('flex', icon.dataset.reviewModalIcon === decision);
            });
            reviewModal.classList.remove('hidden');
            reviewModal.setAttribute('aria-hidden', 'false');
            document.body.classList.add('overflow-hidden');
            window.setTimeout(() => reviewModalCancel?.focus(), 0);
        };

        reviewForms.forEach((form) => {
            form.addEventListener('submit', (event) => {
                if (form.dataset.reviewConfirmed === 'true') {
                    delete form.dataset.reviewConfirmed;
                    return;
                }
                event.preventDefault();
                openReviewModal(form, form.querySelector('[data-review-decision]'));
            });
        });

        reviewModal.querySelectorAll('[data-review-modal-close]').forEach((closeButton) => {
            closeButton.addEventListener('click', closeReviewModal);
        });
        reviewModalCancel?.addEventListener('click', closeReviewModal);
        reviewModalConfirm?.addEventListener('click', () => {
            if (!pendingReviewForm) {
                return;
            }
            const formToSubmit = pendingReviewForm;
            formToSubmit.dataset.reviewConfirmed = 'true';
            closeReviewModal();
            formToSubmit.requestSubmit();
        });
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && !reviewModal.classList.contains('hidden')) {
                closeReviewModal();
            }
        });
    }

    const publishModal = document.querySelector('[data-publish-confirm-modal]');
    const publishForms = [...document.querySelectorAll('[data-publish-form]')];
    if (publishModal && publishForms.length > 0) {
        const publishModalTopicTitle = publishModal.querySelector('[data-publish-modal-topic-title]');
        const publishModalConfirm = publishModal.querySelector('[data-publish-modal-confirm]');
        const publishModalCancel = publishModal.querySelector('[data-publish-modal-cancel]');
        let pendingPublishForm;
        let lastFocusedPublishButton;

        const closePublishModal = () => {
            publishModal.classList.add('hidden');
            publishModal.setAttribute('aria-hidden', 'true');
            document.body.classList.remove('overflow-hidden');
            pendingPublishForm = undefined;
            lastFocusedPublishButton?.focus();
        };

        const openPublishModal = (form, button) => {
            pendingPublishForm = form;
            lastFocusedPublishButton = button;
            if (publishModalTopicTitle) {
                publishModalTopicTitle.textContent = form.dataset.publishTopicTitle || 'this topic';
            }
            publishModal.classList.remove('hidden');
            publishModal.setAttribute('aria-hidden', 'false');
            document.body.classList.add('overflow-hidden');
            window.setTimeout(() => publishModalCancel?.focus(), 0);
        };

        publishForms.forEach((form) => {
            form.addEventListener('submit', (event) => {
                if (form.dataset.publishConfirmed === 'true') {
                    delete form.dataset.publishConfirmed;
                    return;
                }
                event.preventDefault();
                openPublishModal(form, form.querySelector('button[type="submit"]'));
            });
        });

        publishModal.querySelectorAll('[data-publish-modal-close]').forEach((closeButton) => {
            closeButton.addEventListener('click', closePublishModal);
        });
        publishModalCancel?.addEventListener('click', closePublishModal);
        publishModalConfirm?.addEventListener('click', () => {
            if (!pendingPublishForm) {
                return;
            }
            const formToSubmit = pendingPublishForm;
            formToSubmit.dataset.publishConfirmed = 'true';
            closePublishModal();
            formToSubmit.requestSubmit();
        });
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && !publishModal.classList.contains('hidden')) {
                closePublishModal();
            }
        });
    }

    document.querySelectorAll('[data-announcement-scope-form]').forEach((form) => {
        const scope = form.querySelector('[data-announcement-scope]');
        const department = form.querySelector('[data-announcement-department]');
        if (!scope || !department) {
            return;
        }
        const syncDepartment = () => {
            const schoolWide = scope.value === 'SCHOOL';
            department.disabled = schoolWide;
            department.required = !schoolWide;
            if (schoolWide) {
                department.value = '';
            }
        };
        scope.addEventListener('change', syncDepartment);
        syncDepartment();
    });

    const announcementModal = document.querySelector('[data-announcement-confirm-modal]');
    const announcementForms = [...document.querySelectorAll('[data-announcement-action-form]')];
    if (announcementModal && announcementForms.length > 0) {
        const modalTitle = announcementModal.querySelector('[data-announcement-modal-title]');
        const modalDescription = announcementModal.querySelector('[data-announcement-modal-description]');
        const announcementTitle = announcementModal.querySelector('[data-announcement-modal-announcement-title]');
        const confirmButton = announcementModal.querySelector('[data-announcement-modal-confirm]');
        const cancelButton = announcementModal.querySelector('[data-announcement-modal-cancel]');
        let pendingForm;
        let lastFocusedButton;

        const closeModal = () => {
            announcementModal.classList.add('hidden');
            announcementModal.setAttribute('aria-hidden', 'true');
            document.body.classList.remove('overflow-hidden');
            pendingForm = undefined;
            lastFocusedButton?.focus();
        };

        const openModal = (form, button) => {
            const action = form.dataset.announcementAction === 'hide' ? 'hide' : 'publish';
            pendingForm = form;
            lastFocusedButton = button;
            modalTitle.textContent = action === 'hide' ? 'Hide announcement?' : 'Publish announcement?';
            modalDescription.textContent = action === 'hide'
                ? 'End users will no longer see this announcement.'
                : 'End users in the selected scope will be able to see this announcement.';
            announcementTitle.textContent = form.dataset.announcementTitle || 'this announcement';
            confirmButton.textContent = action === 'hide' ? 'Hide announcement' : 'Publish announcement';
            announcementModal.classList.remove('hidden');
            announcementModal.setAttribute('aria-hidden', 'false');
            document.body.classList.add('overflow-hidden');
            window.setTimeout(() => cancelButton?.focus(), 0);
        };

        announcementForms.forEach((form) => {
            form.addEventListener('submit', (event) => {
                if (form.dataset.announcementConfirmed === 'true') {
                    delete form.dataset.announcementConfirmed;
                    return;
                }
                event.preventDefault();
                openModal(form, form.querySelector('button[type="submit"]'));
            });
        });
        announcementModal.querySelectorAll('[data-announcement-modal-close]').forEach((button) => {
            button.addEventListener('click', closeModal);
        });
        cancelButton?.addEventListener('click', closeModal);
        confirmButton?.addEventListener('click', () => {
            if (!pendingForm) {
                return;
            }
            const formToSubmit = pendingForm;
            formToSubmit.dataset.announcementConfirmed = 'true';
            closeModal();
            formToSubmit.requestSubmit();
        });
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && !announcementModal.classList.contains('hidden')) {
                closeModal();
            }
        });
    }

    const topicSubmitModal = document.querySelector('[data-topic-submit-confirm-modal]');
    const topicSubmitForms = [...document.querySelectorAll('[data-submit-review-form]')];
    if (topicSubmitModal && topicSubmitForms.length > 0) {
        const topicTitle = topicSubmitModal.querySelector('[data-topic-submit-modal-topic-title]');
        const confirmButton = topicSubmitModal.querySelector('[data-topic-submit-modal-confirm]');
        const closeButtons = topicSubmitModal.querySelectorAll('[data-topic-submit-modal-close]');
        let pendingSubmitForm;
        let lastFocusedElement;

        const closeTopicSubmitModal = () => {
            topicSubmitModal.classList.add('hidden');
            topicSubmitModal.setAttribute('aria-hidden', 'true');
            document.body.classList.remove('overflow-hidden');
            pendingSubmitForm = undefined;
            lastFocusedElement?.focus();
        };

        document.querySelectorAll('[data-submit-review-open]').forEach((button) => {
            button.addEventListener('click', () => {
                pendingSubmitForm = document.getElementById(button.dataset.submitReviewOpen);
                if (!pendingSubmitForm) {
                    return;
                }
                lastFocusedElement = button;
                if (topicTitle) {
                    topicTitle.textContent = button.dataset.submitReviewTopicTitle || 'this topic';
                }
                topicSubmitModal.classList.remove('hidden');
                topicSubmitModal.setAttribute('aria-hidden', 'false');
                document.body.classList.add('overflow-hidden');
                window.setTimeout(() => confirmButton?.focus(), 0);
            });
        });

        closeButtons.forEach((button) => button.addEventListener('click', closeTopicSubmitModal));
        confirmButton?.addEventListener('click', () => {
            pendingSubmitForm?.submit();
        });
        topicSubmitModal.addEventListener('click', (event) => {
            if (event.target === topicSubmitModal) {
                closeTopicSubmitModal();
            }
        });
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && !topicSubmitModal.classList.contains('hidden')) {
                closeTopicSubmitModal();
            }
        });
    }

    document.querySelectorAll('[data-group-modal]').forEach((modal) => {
        const openButtons = document.querySelectorAll(`[data-group-modal-open="${modal.id}"]`);
        const closeButtons = modal.querySelectorAll('[data-group-modal-close]');
        let lastFocusedElement;

        const closeModal = () => {
            modal.classList.add('hidden');
            modal.setAttribute('aria-hidden', 'true');
            document.body.classList.remove('overflow-hidden');
            lastFocusedElement?.focus();
        };

        const openModal = (event) => {
            lastFocusedElement = event.currentTarget;
            modal.classList.remove('hidden');
            modal.setAttribute('aria-hidden', 'false');
            document.body.classList.add('overflow-hidden');
            const firstField = modal.querySelector('input:not([type="hidden"]):not([disabled]), select:not([disabled]), textarea');
            window.setTimeout(() => firstField?.focus(), 0);
        };

        openButtons.forEach((button) => button.addEventListener('click', openModal));
        closeButtons.forEach((button) => button.addEventListener('click', closeModal));
        modal.addEventListener('click', (event) => {
            if (event.target === modal) {
                closeModal();
            }
        });
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && !modal.classList.contains('hidden')) {
                closeModal();
            }
        });
    });

    const registrationRejectModal = document.querySelector('[data-registration-reject-modal]');
    if (registrationRejectModal) {
        const registrationIdInput = registrationRejectModal.querySelector('[data-registration-rejection-id]');
        const topicLabel = registrationRejectModal.querySelector('[data-registration-reject-topic]');
        const groupLabel = registrationRejectModal.querySelector('[data-registration-reject-group]');
        const reasonInput = registrationRejectModal.querySelector('[name="rejectionReason"]');

        document.querySelectorAll('[data-registration-reject-open]').forEach((button) => {
            button.addEventListener('click', () => {
                if (registrationIdInput) {
                    registrationIdInput.value = button.dataset.registrationId || '';
                }
                if (topicLabel) {
                    topicLabel.textContent = button.dataset.registrationTopic || '';
                }
                if (groupLabel) {
                    groupLabel.textContent = button.dataset.registrationGroup || '';
                }
                if (reasonInput) {
                    reasonInput.value = '';
                }
            });
        });
    }

    document.querySelectorAll('[data-seed-page]').forEach((page) => {
        const csrfInput = page.querySelector('[data-seed-csrf]');
        const runButton = page.querySelector('[data-seed-run]');
        const steps = [...page.querySelectorAll('[data-seed-step]')];
        const status = page.querySelector('[data-seed-status]');
        const feedback = page.querySelector('[data-seed-feedback]');
        const output = page.querySelector('[data-seed-output]');

        const setFeedback = (message, successful) => {
            if (!feedback) {
                return;
            }
            feedback.hidden = false;
            feedback.classList.remove('hidden');
            feedback.textContent = message;
            feedback.classList.toggle('bg-green-50', successful);
            feedback.classList.toggle('text-green-800', successful);
            feedback.classList.toggle('bg-red-50', !successful);
            feedback.classList.toggle('text-red-800', !successful);
        };

        const setStepStatus = (step, label, state) => {
            const stepStatus = step.querySelector('[data-seed-step-status]');
            if (!stepStatus) {
                return;
            }
            stepStatus.textContent = label;
            stepStatus.classList.remove(
                'bg-gray-200', 'text-gray-700',
                'bg-blue-100', 'text-blue-800',
                'bg-green-100', 'text-green-800',
                'bg-red-100', 'text-red-800');
            if (state === 'running') {
                stepStatus.classList.add('bg-blue-100', 'text-blue-800');
            } else if (state === 'success') {
                stepStatus.classList.add('bg-green-100', 'text-green-800');
            } else if (state === 'failed') {
                stepStatus.classList.add('bg-red-100', 'text-red-800');
            } else {
                stepStatus.classList.add('bg-gray-200', 'text-gray-700');
            }
        };

        const requestStep = async (step) => {
            const response = await fetch(step.dataset.seedEndpoint, {
                method: 'POST',
                headers: {
                    Accept: 'application/json',
                    'X-CSRF-TOKEN': csrfInput?.value || ''
                }
            });
            const rawResponse = await response.text();
            let payload;
            try {
                payload = rawResponse ? JSON.parse(rawResponse) : null;
            } catch (error) {
                payload = null;
            }

            if (!response.ok) {
                throw new Error(payload?.message || `${step.dataset.seedLabel} failed (${response.status}).`);
            }
            return payload;
        };

        runButton?.addEventListener('click', async () => {
            if (!window.confirm(runButton.dataset.seedConfirm || 'Run the seed pipeline?')) {
                return;
            }

            runButton.disabled = true;
            steps.forEach((step) => setStepStatus(step, 'Pending', 'pending'));
            if (status) {
                status.textContent = 'Seeding database…';
            }
            if (feedback) {
                feedback.hidden = true;
                feedback.classList.add('hidden');
            }
            if (output) {
                output.hidden = true;
                output.classList.add('hidden');
                output.textContent = '';
            }

            const results = [];
            try {
                for (const step of steps) {
                    setStepStatus(step, 'Running', 'running');
                    const payload = await requestStep(step);
                    results.push({
                        step: step.dataset.seedLabel,
                        endpoint: step.dataset.seedEndpoint,
                        response: payload
                    });
                    setStepStatus(step, 'Completed', 'success');
                }

                if (status) {
                    status.textContent = 'Seed pipeline completed';
                }
                setFeedback('Database schema was recreated and seed data was restored successfully.', true);
                if (output) {
                    output.hidden = false;
                    output.classList.remove('hidden');
                    output.textContent = JSON.stringify(results, null, 2);
                }
            } catch (error) {
                const failedStep = steps.find((step) => step.querySelector('[data-seed-step-status]')?.textContent === 'Running');
                if (failedStep) {
                    setStepStatus(failedStep, 'Failed', 'failed');
                }
                if (status) {
                    status.textContent = 'Seed pipeline failed';
                }
                setFeedback(error.message || 'The seed pipeline failed.', false);
                if (output) {
                    output.hidden = false;
                    output.classList.remove('hidden');
                    output.textContent = JSON.stringify(results, null, 2);
                }
            } finally {
                runButton.disabled = false;
            }
        });
    });
});
