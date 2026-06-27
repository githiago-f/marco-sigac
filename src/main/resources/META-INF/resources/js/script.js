let currentId = null;

function openModal(id, titulo, nomeAluno, tipo, horas, data, status, descricao, limite) {
  currentId = id;

  const content = `
    <div class="card">
      <h3>${titulo}</h3>
      <p><strong>Aluno:</strong> ${nomeAluno}</p>
      <p><strong>Atividade:</strong> ${tipo}</p>
      <div style="margin-top:8px;padding:8px;background:var(--color-muted);border-radius:6px;font-size:13px;">
        <p><strong>Atividade:</strong> ${descricao || tipo}</p>
        <p><strong>Máximo do item:</strong> ${limite || '30'} horas</p>
        <p><strong>Critério:</strong> 1 hora de participação equivale a 1 hora de Atividade Complementar (AC)</p>
        <p><strong>Comprovação:</strong> Atestado ou certificado expedido pela instituição responsável</p>
      </div>
      <p style="margin-top:8px;"><strong>Horas:</strong> ${horas}h</p>
      <p><strong>Data:</strong> ${data}</p>
      <p><strong>Status:</strong> ${status}</p>
    </div>

    <div class="form-group">
      <label>Observações</label>
      <textarea class="input" id="obs" rows="3"></textarea>
      <label>Horas aprovadas</label>
      <input class="input" id="horas" type="number" value="${horas}"/>
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
  const horas = document.getElementById("horas").value;

  fetch(`/atividades/${currentId}/homologado`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `observacoes=${obs.trim() == '' ? 'Aprovado pelo professor' : obs}&horas=${horas}`
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
    body: `observacoes=${obs}&horas=0`
  }).then(() => location.reload());
}
