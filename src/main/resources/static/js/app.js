document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-action="reload"]').forEach((button) => {
        button.addEventListener('click', () => window.location.reload());
    });
});
