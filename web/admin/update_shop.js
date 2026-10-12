async function updateShop(button) {
    button.disabled = true;
    try {
        const send = async payload => {
            const response = await fetch('/admin/ajax_update_shop.php', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({...payload, csrf: shopCsrf})
            });
            return response.json();
        };
        const request = await send({action: 'reload'});
        if (request.status !== 'success') throw new Error(request.msg || 'Không gửi được yêu cầu');
        for (let i = 0; i < 15; i++) {
            await new Promise(resolve => setTimeout(resolve, 1000));
            const result = await send({action: 'status', id: request.id});
            if (result.status !== 'success') throw new Error(result.msg || 'Không kiểm tra được trạng thái');
            if (result.loaded) {
                await Swal.fire('Đã cập nhật shop', `Server đã nạp ${result.count} cửa hàng. Hãy mở lại cửa hàng trong game để thấy thay đổi.`, 'success');
                return;
            }
        }
        throw new Error('Server chưa xác nhận nạp shop. Kiểm tra server đã chạy mã Java mới.');
    } catch (error) {
        Swal.fire('Cập nhật thất bại', error.message, 'error');
    } finally {
        button.disabled = false;
    }
}
