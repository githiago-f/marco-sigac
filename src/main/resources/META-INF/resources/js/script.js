let currentId = null;

function openModal(id, nome, tipo, horas, data, status) {
  currentId = id;

  const content = `
    <div class="card">
      <p><strong>Aluno:</strong> ${nome}</p>
      <p><strong>Atividade:</strong> ${tipo}</p>
      <p><strong>Horas:</strong> ${horas}h</p>
      <p><strong>Data:</strong> ${data}</p>
      <p><strong>Status:</strong> ${status}</p>
    </div>

    <div class="form-group">
      <label>Observações</label>
      <textarea class="input" id="obs" rows="3"></textarea>
    </div>
  `;

  document.getElementById('modal-content').innerHTML = content;
  document.getElementById('modal').classList.add('open');
}

function closeModal() {
  document.getElementById('modal').classList.remove('open');
}

function aprovar() {
  const obs = document.getElementById('obs').value;

  fetch(`/atividades/${currentId}/homologado`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `observacoes=${obs.trim() == '' ? 'Aprovado pelo professor' : obs}`
  }).then(() => location.reload());
}

function rejeitar() {
  const obs = document.getElementById('obs').value;

  if (!obs.trim()) {
    alert('Informe o motivo da rejeição');
    return;
  }

  fetch(`/atividades/${currentId}/rejeitado`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `observacoes=${obs}`
  }).then(() => location.reload());
}
