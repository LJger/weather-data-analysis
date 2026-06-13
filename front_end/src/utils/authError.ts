const TECHNICAL_ERROR_PATTERN =
  /###|SQL:|PSQLException|Error updating database|DuplicateKey|DataIntegrity|constraint|org\.|java\.|重复键|唯一约束/i

export const normalizeAuthErrorMessage = (message: unknown, fallback: string) => {
  if (typeof message !== 'string' || !message.trim()) {
    return fallback
  }

  if (/sys_user_email_key|\(email\)=|邮箱.*(已存在|已被注册)/i.test(message)) {
    return '该邮箱已被注册，请直接登录或更换邮箱'
  }

  if (/sys_user_phone_key|\(phone\)=|手机号.*(已存在|已被注册)/i.test(message)) {
    return '该手机号已被注册，请更换手机号'
  }

  if (/sys_user_username_key|\(username\)=|用户名.*已存在/i.test(message)) {
    return '用户名已存在'
  }

  if (TECHNICAL_ERROR_PATTERN.test(message)) {
    return fallback
  }

  return message
}

export const getAuthErrorMessage = (error: any, fallback: string) => {
  return normalizeAuthErrorMessage(error?.response?.data?.message || error?.message, fallback)
}
