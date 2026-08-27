document.addEventListener('DOMContentLoaded', () => {
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
        const toggle = group.querySelector('[data-group-toggle]');

        if (trigger && body) {
            trigger.addEventListener('click', () => {
                const isExpanded = trigger.getAttribute('aria-expanded') === 'true';
                trigger.setAttribute('aria-expanded', String(!isExpanded));
                group.classList.toggle('is-collapsed', isExpanded);
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
});
