import dayjs from 'dayjs'

export const formatDateTime = (value) => {
  if (!value) return ''
  return dayjs(value).format('YYYY-MM-DD HH:mm')
}

export const formatPrice = (value) => {
  if (value === null || value === undefined) return '--'
  return `${value}`
}
