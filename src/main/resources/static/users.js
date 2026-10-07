document.addEventListener('DOMContentLoaded', function () {
    const filasUsuarios = document.querySelectorAll('.usuario-fila');

    filasUsuarios.forEach(function (fila) {
        fila.addEventListener('click', function () {
            const color = this.dataset.color;
            document.body.style.backgroundColor = color;
        });
    });

    const botonesEliminar = document.querySelectorAll('.eliminar-btn');

    botonesEliminar.forEach(function (boton) {
        boton.addEventListener('click', async function () {
            const id = this.dataset.id;
            const confirmar = confirm('¿Quieres quitar este usuario?');
            if (!confirmar) {
                return;
            }

            try {
                const response = await fetch('/users/' + id, {
                    method: 'DELETE'
                });
                if (!response.ok) {
                    throw new Error('No se pudo eliminar el usuario');
                }
                alert('Usuario eliminado correctamente.');
                window.location.reload();
            } catch (error) {
                alert(error.message);
            }
        });
    });
});